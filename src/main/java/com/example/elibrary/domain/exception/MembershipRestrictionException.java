package com.example.elibrary.domain.exception;

/** Borrowing attempt rejected because the member violates a prerequisite rule. */
public class MembershipRestrictionException extends BusinessException {

    public MembershipRestrictionException(BusinessErrorCode code, String message) {
        super(code, message);
    }

    public static MembershipRestrictionException quotaExceeded(int limit) {
        return new MembershipRestrictionException(BusinessErrorCode.LOAN_LIMIT_EXCEEDED,
                "Cannot exceed " + limit + " concurrent loans");
    }

    public static MembershipRestrictionException suspended(Long memberId) {
        return new MembershipRestrictionException(BusinessErrorCode.MEMBER_RESTRICTED,
                "Member " + memberId + " is suspended");
    }

    public static MembershipRestrictionException outstandingFine(int count, String amount) {
        return new MembershipRestrictionException(BusinessErrorCode.OUTSTANDING_FINE,
                "Member has " + count + " outstanding fine(s) totalling " + amount);
    }

    public static MembershipRestrictionException renewalNotAllowed(Long loanId, String reason) {
        return new MembershipRestrictionException(BusinessErrorCode.RENEWAL_NOT_ALLOWED,
                "Loan " + loanId + " cannot be renewed: " + reason);
    }
}
