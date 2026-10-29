package com.example.elibrary.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Emission of every loan lifecycle event (immutable records). */
public final class LoanEvents {

    private LoanEvents() {
    }

    public static String newEventId() {
        return UUID.randomUUID().toString();
    }

    public record Borrowed(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            Instant occurredAt,
            Long memberId,
            Long bookId,
            Long copyId,
            Instant dueAt) implements DomainEvent {
    }

    public record Returned(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            Instant occurredAt,
            Long memberId,
            Long bookId,
            Long copyId,
            int daysOverdue) implements DomainEvent {
    }

    public record Overdue(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            Instant occurredAt,
            Long memberId,
            Long bookId,
            Instant dueAt) implements DomainEvent {
    }

    public record Renewed(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            Instant occurredAt,
            Long memberId,
            Long bookId,
            Instant previousDueAt,
            Instant newDueAt,
            int renewCount) implements DomainEvent {
    }
}
