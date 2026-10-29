package com.example.elibrary.domain.model;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 借阅状态的生命周期。迁移由 {@link Loan} 把关（见 {@link Loan#tryTransitTo}）。
 */
public enum LoanStatus {

    /** 借出，还没到期。 */
    ACTIVE,
    /** 借出且已逾期（由定时任务算出，不手工设置）。 */
    OVERDUE,
    /** 已归还——终态，出去就回不来。 */
    RETURNED;

    /** 这几类状态仍然占着会员的借阅额度。 */
    public static final Set<LoanStatus> OPEN_STATUSES =
            Stream.of(ACTIVE, OVERDUE).collect(Collectors.toUnmodifiableSet());

    public boolean isOpen() {
        return OPEN_STATUSES.contains(this);
    }
}
