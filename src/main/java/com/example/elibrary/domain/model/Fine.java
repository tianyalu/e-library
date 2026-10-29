package com.example.elibrary.domain.model;

import com.example.elibrary.domain.money.Money;
import lombok.Getter;

/** 一笔逾期的罚金记录。每笔借阅最多一张罚单（库里有约束保证）。 */
@Getter
public class Fine {

    private Long id;
    private Long loanId;
    private Long memberId;
    private Money amount;
    private int daysOverdue;
    private FineStatus status;
    private Long version;

    private Fine() {
    }

    private Fine(Long id, Long loanId, Long memberId, Money amount, int daysOverdue, FineStatus status, Long version) {
        this.id = id;
        this.loanId = loanId;
        this.memberId = memberId;
        this.amount = amount;
        this.daysOverdue = daysOverdue;
        this.status = status;
        this.version = version;
    }

    public static Fine create(Long loanId, Long memberId, Money amount, int daysOverdue) {
        if (daysOverdue <= 0) {
            throw new IllegalArgumentException("Fine requires positive overdue days, got " + daysOverdue);
        }
        return new Fine(null, loanId, memberId, amount, daysOverdue, FineStatus.PENDING, null);
    }

    public static Fine restore(Long id, Long loanId, Long memberId, Money amount, int daysOverdue,
                               FineStatus status, Long version) {
        return new Fine(id, loanId, memberId, amount, daysOverdue, status, version);
    }

    /** 让持久层把自增 ID 回填进来。 */
    public void assignIdForNewPersistence(Long generatedId) {
        this.id = generatedId;
    }

    /** 这笔罚金是否还在挡着借阅。 */
    public boolean outstanding() {
        return status == FineStatus.PENDING;
    }

    public void settle(FineStatus target) {
        if (status != FineStatus.PENDING) {
            throw new IllegalStateException("Fine " + id + " already settled as " + status);
        }
        if (target == FineStatus.PENDING) {
            throw new IllegalArgumentException("Cannot settle a fine back to PENDING");
        }
        status = target;
    }
}
