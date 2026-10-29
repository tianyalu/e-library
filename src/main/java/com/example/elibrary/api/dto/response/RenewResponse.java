package com.example.elibrary.api.dto.response;

import java.time.Instant;

public record RenewResponse(long loanId, Instant previousDueAt, Instant newDueAt, int renewCount) {
}
