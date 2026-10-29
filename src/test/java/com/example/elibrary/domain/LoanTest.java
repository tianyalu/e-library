package com.example.elibrary.domain;

import com.example.elibrary.application.policy.LibraryLoanPolicy;
import com.example.elibrary.domain.exception.BusinessErrorCode;
import com.example.elibrary.domain.exception.ConflictException;
import com.example.elibrary.domain.exception.MembershipRestrictionException;
import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.model.LoanStatus;
import com.example.elibrary.domain.policy.LoanPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 聚合行为测试。不起 Spring、不连库：被测单元就是那些不变量本身。
 */
class LoanTest {

    private static final Instant BORROWED_AT = Instant.parse("2026-03-01T10:00:00Z");

    private LoanPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new LibraryLoanPolicy(14, 7, 1, 5, new BigDecimal("0.50"), "CNY");
    }

    private Loan newLoan() {
        return Loan.open(1L, 10L, 100L, BORROWED_AT, policy);
    }

    @Test
    @DisplayName("按时归还会关闭借阅，不产生罚金")
    void returnOnTime() {
        Loan loan = newLoan();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getDueAt()).isEqualTo(BORROWED_AT.plus(14, ChronoUnit.DAYS));

        Optional<Fine> fine = loan.returnItem(loan.getDueAt().minusSeconds(1), policy);

        assertThat(fine).isEmpty();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(loan.getDaysOverdue()).isZero();
    }

    @Test
    @DisplayName("逾期归还要按每个已开始的天数计费")
    void lateReturnCreatesFine() {
        Loan loan = newLoan();
        Instant returnedAt = loan.getDueAt().plus(3, ChronoUnit.DAYS).plusSeconds(30);

        Optional<Fine> fine = loan.returnItem(returnedAt, policy);

        assertThat(fine).isPresent();
        assertThat(fine.get().getDaysOverdue()).isEqualTo(4);
        assertThat(fine.get().getAmount().toString()).isEqualTo("CNY 2.00");
        assertThat(fine.get().outstanding()).isTrue();
    }

    @Test
    @DisplayName("不能还两次：RETURNED 是终态")
    void doubleReturnRejected() {
        Loan loan = newLoan();
        loan.returnItem(loan.getDueAt(), policy);

        assertThatThrownBy(() -> loan.returnItem(loan.getDueAt().plusSeconds(1), policy))
                .isInstanceOf(ConflictException.class)
                .extracting(exception -> ((ConflictException) exception).code())
                .isEqualTo(BusinessErrorCode.ALREADY_RETURNED);
    }

    @Test
    @DisplayName("逾期状态迁移是幂等的，重复扫描不会改变任何东西")
    void overdueTransitionIsIdempotent() {
        Loan loan = newLoan();
        Instant late = loan.getDueAt().plusSeconds(1);

        assertThat(loan.markOverdueIfNeeded(late)).isTrue();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        assertThat(loan.markOverdueIfNeeded(late)).isFalse();
        assertThat(loan.markOverdueIfNeeded(loan.getDueAt().minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("续借次数上限由聚合自己保证")
    void renewalQuota() {
        Loan loan = newLoan();
        loan.renew(BORROWED_AT, policy);
        assertThat(loan.getRenewCount()).isEqualTo(1);

        assertThatThrownBy(() -> loan.renew(BORROWED_AT.plus(1, ChronoUnit.DAYS), policy))
                .isInstanceOf(MembershipRestrictionException.class);
    }

    @Test
    @DisplayName("逾期借阅续借后回到 ACTIVE，并拿到新的到期日")
    void renewalOfOverdueLoan() {
        Loan loan = newLoan();
        Instant late = loan.getDueAt().plus(5, ChronoUnit.DAYS);
        loan.markOverdueIfNeeded(late);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);

        loan.renew(late, policy);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getDueAt()).isAfter(late);
    }
}
