package com.example.elibrary.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Borrowing mutation payload. It is also the object hashed for idempotency, so every future
 * field must have well-defined serialisation semantics.
 */
public record BorrowRequest(@NotNull @Positive Long bookId) {
}
