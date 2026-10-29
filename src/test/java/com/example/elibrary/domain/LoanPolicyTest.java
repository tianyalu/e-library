package com.example.elibrary.domain;

import com.example.elibrary.application.policy.LibraryLoanPolicy;
import com.example.elibrary.domain.policy.LoanPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Time-based rules are pure functions, so they are tested without Spring and without a clock. */
class LoanPolicyTest {

    private LoanPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new LibraryLoanPolicy(14, 7, 1, 5, new BigDecimal("0.50"), "CNY");
    }

    @Test
    @DisplayName("due date is borrowedAt + loan period")
    void dueDate() {
        Instant borrowedAt = Instant.parse("2026-01-01T00:00:00Z");
        assertThat(policy.dueDateFrom(borrowedAt)).isEqualTo("2026-01-15T00:00:00Z");
    }

    @Test
    @DisplayName("renewal never shortens the loan: it anchors on max(dueAt, now)")
    void renewalAnchors() {
        Instant dueAt = Instant.parse("2026-01-15T00:00:00Z");
        Instant lateNow = Instant.parse("2026-02-01T00:00:00Z");
        assertThat(policy.extendedDueDate(dueAt, lateNow)).isEqualTo(lateNow.plus(7, ChronoUnit.DAYS));

        Instant earlyNow = Instant.parse("2026-01-02T00:00:00Z");
        assertThat(policy.extendedDueDate(dueAt, earlyNow)).isEqualTo(dueAt.plus(7, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("a started day is already charged as one full day")
    void overdueDaysCeil() {
        Instant dueAt = Instant.parse("2026-01-15T00:00:00Z");
        assertThat(policy.overdueDays(dueAt, dueAt)).isZero();
        assertThat(policy.overdueDays(dueAt, dueAt.plusSeconds(1))).isEqualTo(1);
        assertThat(policy.overdueDays(dueAt, dueAt.plus(5, ChronoUnit.SECONDS))).isEqualTo(1);
        assertThat(policy.overdueDays(dueAt, dueAt.plus(2, ChronoUnit.DAYS))).isEqualTo(2);
    }

    @Test
    @DisplayName("penalty is days * rate and zero when on time")
    void fineCalculation() {
        assertThat(policy.fineFor(0).isZero()).isTrue();
        assertThat(policy.fineFor(3).toString()).isEqualTo("CNY 1.50");
    }
}
