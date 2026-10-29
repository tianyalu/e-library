package com.example.elibrary.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金额值对象。三条硬规则写死在这里：
 * <ul>
 *   <li>不用 {@code double} / {@code float}，一律 {@link BigDecimal} 且保留 2 位；</li>
 *   <li>舍入模式明确写成 HALF_UP，不吃框架默认值；</li>
 *   <li>币种必填，不同币种的金额不能相加，直接报错。</li>
 * </ul>
 */
public final class Money implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = Objects.requireNonNull(amount, "amount").setScale(SCALE, ROUNDING_MODE);
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public Money multiply(long times) {
        return new Money(amount.multiply(BigDecimal.valueOf(times)), currency);
    }

    public Money multiply(BigDecimal times) {
        return new Money(amount.multiply(times), currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        return compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }
}
