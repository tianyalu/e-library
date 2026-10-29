package com.example.elibrary.domain.policy;

import com.example.elibrary.domain.money.Money;

import java.time.Instant;

/**
 * 借阅规则里可配置的那些部分（借期、额度、罚金率…）。
 *
 * <p>领域只依赖这个抽象，不写死数字，好处是规则能按环境调，而且每条规则都能单独单测。
 */
public interface LoanPolicy {

    /** 借出时间 -> 到期时间。 */
    Instant dueDateFrom(Instant borrowedAt);

    /** 续借 -> 新到期时间，锚点是 {@code max(当前到期日, 现在)}，避免把到期日越续越短。 */
    Instant extendedDueDate(Instant currentDueAt, Instant now);

    int maxRenewals();

    int maxActiveLoans();

    /**
     * 应还日到实际归还日之间要计费的逾期天数。
     * "开头算一天"，即不足一天按一天计。
     */
    long overdueDays(Instant dueAt, Instant returnedAt);

    /** 逾期这么多天要罚多少。 */
    Money fineFor(long overdueDays);

    /** 此刻是否已经逾期。 */
    default boolean isOverdue(Instant now, Instant dueAt) {
        return now.isAfter(dueAt);
    }
}
