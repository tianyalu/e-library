package com.example.elibrary.domain.model;

import com.example.elibrary.domain.exception.InvalidStateTransitionException;
import com.example.elibrary.domain.policy.LoanPolicy;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;

/**
 * 借阅合约：一个会员借走一本馆藏副本。
 *
 * <p>合法的状态变化才写成方法，非法的直接抛异常，不走 setter 自由赋值，
 * 这样构造不出"已归还但又被续借"这类不可能状态。迁移规则见 {@link LoanStatus}。
 */
@Getter
public class Loan {

    private Long id;
    private Long memberId;
    private Long bookId;
    private Long copyId;
    private Instant borrowedAt;
    private Instant dueAt;
    private Instant returnedAt;
    private LoanStatus status;
    private int renewCount;
    private int daysOverdue;
    private Long version;

    private Loan() {
    }

    private Loan(Long id, Long memberId, Long bookId, Long copyId, Instant borrowedAt, Instant dueAt,
                 Instant returnedAt, LoanStatus status, int renewCount, int daysOverdue, Long version) {
        this.id = id;
        this.memberId = memberId;
        this.bookId = bookId;
        this.copyId = copyId;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.returnedAt = returnedAt;
        this.status = status;
        this.renewCount = renewCount;
        this.daysOverdue = daysOverdue;
        this.version = version;
    }

    public static Loan open(Long memberId, Long bookId, Long copyId, Instant now, LoanPolicy policy) {
        return new Loan(null, memberId, bookId, copyId, now, policy.dueDateFrom(now), null,
                LoanStatus.ACTIVE, 0, 0, null);
    }

    /** 持久层把已有行还原成对象时用。 */
    public static Loan restore(Long id, Long memberId, Long bookId, Long copyId, Instant borrowedAt, Instant dueAt,
                               Instant returnedAt, LoanStatus status, int renewCount, int daysOverdue, Long version) {
        return new Loan(id, memberId, bookId, copyId, borrowedAt, dueAt, returnedAt, status, renewCount,
                daysOverdue, version);
    }

    public void assignIdForNewPersistence(Long generatedId) {
        this.id = generatedId;
    }

    public boolean isReturned() {
        return status == LoanStatus.RETURNED;
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    /**
     * 逾期扫描会反复跑同一批数据，所以这个方法要幂等：已经是逾期或已归还都不报错。
     *
     * @return 本次是否真的改了状态
     */
    public boolean markOverdueIfNeeded(Instant now) {
        if (status == LoanStatus.ACTIVE && now.isAfter(dueAt)) {
            status = LoanStatus.OVERDUE;
            return true;
        }
        return false;
    }

    /**
     * 续借。ACTIVE 和 OVERDUE 都允许（逾期后续借会把它拉回 ACTIVE），
     * 已归还或次数用尽则拒绝。
     */
    public void renew(Instant now, LoanPolicy policy) {
        if (status == LoanStatus.RETURNED) {
            throw new InvalidStateTransitionException("Loan", String.valueOf(id), status, LoanStatus.ACTIVE);
        }
        if (renewCount >= policy.maxRenewals()) {
            throw new com.example.elibrary.domain.exception.MembershipRestrictionException(
                    com.example.elibrary.domain.exception.BusinessErrorCode.RENEWAL_NOT_ALLOWED,
                    "loanId=" + id + " already renewed " + renewCount + " time(s), limit is " + policy.maxRenewals());
        }
        dueAt = policy.extendedDueDate(dueAt, now);
        renewCount++;
        if (status == LoanStatus.OVERDUE) {
            status = LoanStatus.ACTIVE;
        }
    }

    /**
     * 还书。
     *
     * @return 逾期时要开的罚单；按时归还是 {@link Optional#empty()}
     */
    public Optional<Fine> returnItem(Instant now, LoanPolicy policy) {
        if (status == LoanStatus.RETURNED) {
            throw new com.example.elibrary.domain.exception.ConflictException(
                    com.example.elibrary.domain.exception.BusinessErrorCode.ALREADY_RETURNED,
                    "loanId=" + id + " has already been returned at " + returnedAt);
        }
        returnedAt = now;
        status = LoanStatus.RETURNED;
        long late = policy.overdueDays(dueAt, now);
        if (late <= 0) {
            daysOverdue = 0;
            return Optional.empty();
        }
        daysOverdue = (int) late;
        return Optional.of(Fine.create(id, memberId, policy.fineFor(late), daysOverdue));
    }
}
