package com.example.elibrary.application.policy;

import com.example.elibrary.domain.money.Money;
import com.example.elibrary.domain.policy.LoanPolicy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * {@link LoanPolicy} 的实现，参数全部来自外部配置。
 *
 * <p>时间计算一律用 {@link Instant}，JVM 或服务器时区怎么设都不影响行为，
 * 只有展示层才转成具体时区。
 */
public class LibraryLoanPolicy implements LoanPolicy {

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    private final int loanPeriodDays;
    private final int renewalDays;
    private final int maxRenewals;
    private final int maxActiveLoans;
    private final BigDecimal finePerDay;
    private final String currency;

    public LibraryLoanPolicy(int loanPeriodDays,
                             int renewalDays,
                             int maxRenewals,
                             int maxActiveLoans,
                             BigDecimal finePerDay,
                             String currency) {
        this.loanPeriodDays = positive(loanPeriodDays, "loanPeriodDays");
        this.renewalDays = positive(renewalDays, "renewalDays");
        this.maxRenewals = maxRenewals;
        this.maxActiveLoans = positive(maxActiveLoans, "maxActiveLoans");
        this.finePerDay = Objects.requireNonNull(finePerDay, "finePerDay");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    @Override
    public Instant dueDateFrom(Instant borrowedAt) {
        return borrowedAt.plus(loanPeriodDays, ChronoUnit.DAYS);
    }

    @Override
    public Instant extendedDueDate(Instant currentDueAt, Instant now) {
        Instant anchor = currentDueAt.isAfter(now) ? currentDueAt : now;
        return anchor.plus(renewalDays, ChronoUnit.DAYS);
    }

    @Override
    public int maxRenewals() {
        return maxRenewals;
    }

    @Override
    public int maxActiveLoans() {
        return maxActiveLoans;
    }

    /**
     * "开头算一天"：晚 1 秒也算一整天。
     * 全程秒的整数运算，不碰浮点。
     */
    @Override
    public long overdueDays(Instant dueAt, Instant returnedAt) {
        long seconds = Duration.between(dueAt, returnedAt).getSeconds();
        if (seconds <= 0) {
            return 0L;
        }
        long days = seconds / SECONDS_PER_DAY;
        return seconds % SECONDS_PER_DAY == 0 ? days : days + 1;
    }

    @Override
    public Money fineFor(long overdueDays) {
        if (overdueDays <= 0) {
            return Money.zero(currency);
        }
        return Money.of(finePerDay.multiply(BigDecimal.valueOf(overdueDays)), currency);
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, got " + value);
        }
        return value;
    }
}
