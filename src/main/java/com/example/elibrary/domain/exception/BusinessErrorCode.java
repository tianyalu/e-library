package com.example.elibrary.domain.exception;

/**
 * 面向业务的错误码，属于 API 的公开契约：调用方可以拿 {@code code} 做分支判断，
 * 哪怕它只是把 {@code detail} 展示给用户。
 *
 * <p>这里只出现 HTTP 状态码的数字，不引入任何 HTTP 抽象，这样 domain 也能被批处理、
 * RPC、消息监听复用。
 */
public enum BusinessErrorCode {

    RESOURCE_NOT_FOUND(404, "Resource not found"),
    INVALID_CREDENTIALS(401, "Invalid credentials"),
    COPY_NOT_AVAILABLE(409, "No copy of this book is currently available"),
    DUPLICATE_ACTIVE_LOAN(409, "You already have an open loan for this book"),
    ALREADY_RETURNED(409, "Loan has already been returned"),
    OPTIMISTIC_CONFLICT(409, "Concurrent modification detected, please retry"),
    LOAN_LIMIT_EXCEEDED(422, "Borrowing quota exceeded"),
    MEMBER_RESTRICTED(403, "Member is not allowed to borrow"),
    OUTSTANDING_FINE(422, "Member has an unpaid fine"),
    RENEWAL_NOT_ALLOWED(422, "Loan cannot be renewed"),
    INVALID_STATE_TRANSITION(409, "Illegal state transition"),
    IDEMPOTENCY_KEY_REUSED(409, "Idempotency key reused with a different payload");

    private final int statusCode;
    private final String title;

    BusinessErrorCode(int statusCode, String title) {
        this.statusCode = statusCode;
        this.title = title;
    }

    public int statusCode() {
        return statusCode;
    }

    public String title() {
        return title;
    }
}
