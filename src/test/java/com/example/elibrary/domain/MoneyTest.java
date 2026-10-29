package com.example.elibrary.domain;

import com.example.elibrary.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 金额是"财务正确性"最小的一环，所以测得最狠：舍入、精度、币种混算，
 * 钱在那儿悄悄蒸发就是从这些地方开始的。
 */
class MoneyTest {

    @Test
    @DisplayName("金额一律保留 2 位小数，按 HALF_UP 处理")
    void roundsToTwoDecimals() {
        Money money = Money.of(new BigDecimal("0.505"), "CNY");
        assertThat(money.amount()).isEqualByComparingTo("0.51");
        assertThat(money.amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("不同币种的金额相加会报错，而不是悄悄换算")
    void rejectsCurrencyMismatch() {
        assertThatThrownBy(() -> Money.of("1.00", "CNY").plus(Money.of("1.00", "USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("罚金随天数线性增长且精确计算，没有浮点漂移")
    void fineIsExact() {
        Money fine = Money.of(new BigDecimal("0.50"), "CNY").multiply(3L);
        assertThat(fine.toString()).isEqualTo("CNY 1.50");
        assertThat(Money.zero("CNY").isZero()).isTrue();
    }
}
