package com.example.elibrary.api.dto.response;

import java.time.Instant;

/**
 * Result of a return operation.
 *
 * @param fineAmount     penalty raised for this return, {@code null} when returned in time
 * @param alreadyReturned {@code true} when this call did nothing because the loan was closed before
 */
public record ReturnResponse(long loanId,
                             long bookId,
                             Instant returnedAt,
                             int daysOverdue,
                             String fineAmount,
                             boolean alreadyReturned) {
}
