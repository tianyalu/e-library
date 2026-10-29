package com.example.elibrary.domain.model;

/** Account state of a member. */
public enum MemberStatus {
    ACTIVE,
    /** Barred from borrowing until reactivated. */
    SUSPENDED
}
