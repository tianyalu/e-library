package com.example.elibrary.application.dto;

import com.example.elibrary.domain.model.LoanStatus;

import java.time.Instant;

/**
 * Read model of a loan: the stored contract line plus derived fields
 * ({@code overdue} for open loans, actual {@code overdueDays} once returned).
 *
 * <p>A dedicated read model keeps API responses stable even when the aggregate changes.
 */
public record LoanView(Long loanId,
                       Long bookId,
                       String bookTitle,
                       Long copyId,
                       Instant borrowedAt,
                       Instant dueAt,
                       Instant returnedAt,
                       LoanStatus status,
                       boolean overdue,
                       long overdueDays,
                       int renewCount) {
}
