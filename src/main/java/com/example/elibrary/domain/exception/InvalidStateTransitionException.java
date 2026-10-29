package com.example.elibrary.domain.exception;

import com.example.elibrary.domain.model.LoanStatus;

/** Raised by the domain when a state machine refuses a transition. */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(String aggregate, Object id, String from, String to) {
        super(BusinessErrorCode.INVALID_STATE_TRANSITION,
                aggregate + " " + id + " cannot go from " + from + " to " + to);
    }

    public InvalidStateTransitionException(String aggregate, Object id, LoanStatus from, LoanStatus to) {
        this(aggregate, id, String.valueOf(from), String.valueOf(to));
    }
}
