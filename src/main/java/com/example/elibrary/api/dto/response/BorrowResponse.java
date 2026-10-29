package com.example.elibrary.api.dto.response;

import java.time.Instant;

/** Result of a successful borrowing. Also the shape cached for idempotent replays. */
public record BorrowResponse(long loanId,
                             long bookId,
                             String bookTitle,
                             String copyBarcode,
                             Instant borrowedAt,
                             Instant dueAt) {
}
