package com.example.elibrary.application.usecase;

import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.application.port.out.CopyRepositoryPort;
import com.example.elibrary.application.port.out.FineRepositoryPort;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.application.port.out.MemberRepositoryPort;
import com.example.elibrary.application.port.out.OutboxRepositoryPort;
import com.example.elibrary.domain.event.LoanEvents;
import com.example.elibrary.domain.exception.ConflictException;
import com.example.elibrary.domain.exception.MembershipRestrictionException;
import com.example.elibrary.domain.exception.ResourceNotFoundException;
import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.FineStatus;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.model.Member;
import com.example.elibrary.domain.money.Money;
import com.example.elibrary.domain.policy.LoanPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 借书——并发最激烈的操作：不能超卖、不能产生重复合约、不能出现"扣了库存却没借到书"。
 *
 * <p>事务边界：规则校验 → 原子扣库存 → 锁副本行 → 写借阅 → 写发件箱，五步一个事务，
 * 要么全成要么全滚，不存在"书被占着但借阅记录没有"的中间态。
 *
 * <p>并发靠四道防线：
 * <ol>
 *   <li>那条带条件的 UPDATE 才是真正的闸门：库存有几本，就恰好几个赢家；</li>
 *   <li>副本行 {@code SELECT ... FOR UPDATE} 串行落在同一本实物上的写者；</li>
 *   <li>库里的部分唯一索引 {@code UNIQUE(member_id, book_id, active_flag)} 兜底：
 *       就算应用层检查全绕过，也插不进第二笔在借记录；</li>
 *   <li>每行的 {@code @Version} 拦住任何改动同一行的人。</li>
 * </ol>
 */
@Slf4j
@Service
public class BorrowBookUseCase {

    public record Result(long loanId,
                         long bookId,
                         String bookTitle,
                         String copyBarcode,
                         Instant borrowedAt,
                         Instant dueAt) {
    }

    private final BookRepositoryPort books;
    private final CopyRepositoryPort copies;
    private final MemberRepositoryPort members;
    private final LoanRepositoryPort loans;
    private final FineRepositoryPort fines;
    private final OutboxRepositoryPort outbox;
    private final LoanPolicy policy;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public BorrowBookUseCase(BookRepositoryPort books,
                             CopyRepositoryPort copies,
                             MemberRepositoryPort members,
                             LoanRepositoryPort loans,
                             FineRepositoryPort fines,
                             OutboxRepositoryPort outbox,
                             LoanPolicy policy,
                             Clock clock,
                             MeterRegistry meterRegistry) {
        this.books = books;
        this.copies = copies;
        this.members = members;
        this.loans = loans;
        this.fines = fines;
        this.outbox = outbox;
        this.policy = policy;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result execute(Long memberId, Long bookId) {
        Instant now = clock.instant();

        Member member = members.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
        Book book = books.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        assertAllowedToBorrow(member, bookId);

        if (!books.debitAvailableCopies(bookId)) {
            recordOutcome("no-stock");
            throw ConflictException.copyNotAvailable(bookId);
        }
        Optional<BookCopy> maybeCopy = copies.findFirstAvailableForUpdate(bookId);
        if (maybeCopy.isEmpty()) {
            // 账上显示有货却挑不出实物副本 = 账实已经漂移。先把计数器加回去，
            // 不让偏差继续累积，漂移本身交给对账任务去告警。
            books.creditAvailableCopies(bookId);
            recordOutcome("inventory-drift");
            throw ConflictException.copyNotAvailable(bookId);
        }

        BookCopy copy = maybeCopy.get();
        copy.lend();
        copies.save(copy);

        Loan loan = loans.save(Loan.open(memberId, bookId, copy.getId(), now, policy));

        outbox.append(new LoanEvents.Borrowed(
                LoanEvents.newEventId(), "loan.borrowed", "Loan", String.valueOf(loan.getId()), now,
                memberId, bookId, copy.getId(), loan.getDueAt()));

        recordOutcome("success");
        log.info("Borrowed bookId={} copyId={} memberId={} loanId={} dueAt={}",
                bookId, copy.getId(), memberId, loan.getId(), loan.getDueAt());
        return new Result(loan.getId(), bookId, book.getTitle(), copy.getBarcode(), loan.getBorrowedAt(),
                loan.getDueAt());
    }

    /** 借阅前置校验，按开销从小到大排。 */
    private void assertAllowedToBorrow(Member member, Long bookId) {
        if (!member.canBorrow()) {
            recordOutcome("member-suspended");
            throw MembershipRestrictionException.suspended(member.getId());
        }
        if (loans.existsOpenLoan(member.getId(), bookId)) {
            recordOutcome("duplicate");
            throw ConflictException.duplicateActiveLoan(bookId);
        }
        long openLoans = loans.countOpenLoans(member.getId());
        if (openLoans >= policy.maxActiveLoans()) {
            recordOutcome("quota");
            throw MembershipRestrictionException.quotaExceeded(policy.maxActiveLoans());
        }
        List<Fine> outstanding = fines.findByMemberAndStatus(member.getId(), FineStatus.PENDING);
        if (!outstanding.isEmpty()) {
            recordOutcome("unpaid-fine");
            String currency = outstanding.get(0).getAmount().currency();
            Money total = outstanding.stream()
                    .map(Fine::getAmount)
                    .reduce(Money.zero(currency), Money::plus);
            throw MembershipRestrictionException.outstandingFine(outstanding.size(), total.toString());
        }
    }

    private void recordOutcome(String outcome) {
        meterRegistry.counter("elibrary.loan.borrow", "outcome", outcome).increment();
    }
}
