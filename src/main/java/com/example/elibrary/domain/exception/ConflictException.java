package com.example.elibrary.domain.exception;

/** Conflict / rule violation. Kept generic on purpose; the code carries the specific meaning. */
public class ConflictException extends BusinessException {

    public ConflictException(BusinessErrorCode code, String message) {
        super(code, message);
    }

    public static ConflictException copyNotAvailable(Long bookId) {
        return new ConflictException(BusinessErrorCode.COPY_NOT_AVAILABLE,
                "No available copy for bookId=" + bookId + " at the moment");
    }

    public static ConflictException duplicateActiveLoan(Long bookId) {
        return new ConflictException(BusinessErrorCode.DUPLICATE_ACTIVE_LOAN,
                "Member already holds an open loan for bookId=" + bookId);
    }

    public static ConflictException alreadyReturned(Long loanId) {
        return new ConflictException(BusinessErrorCode.ALREADY_RETURNED, "LoanId=" + loanId + " already returned");
    }

    public static ConflictException optimisticConflict(String entity, Object id) {
        return new ConflictException(BusinessErrorCode.OPTIMISTIC_CONFLICT,
                entity + " " + id + " was modified concurrently");
    }

    public static ConflictException idempotencyKeyReused(String key) {
        return new ConflictException(BusinessErrorCode.IDEMPOTENCY_KEY_REUSED,
                "Idempotency-Key '" + key + "' was already used with a different payload");
    }
}
