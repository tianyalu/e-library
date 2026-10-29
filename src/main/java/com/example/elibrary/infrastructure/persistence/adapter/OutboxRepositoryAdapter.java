package com.example.elibrary.infrastructure.persistence.adapter;

import com.example.elibrary.application.port.out.OutboxRepositoryPort;
import com.example.elibrary.domain.event.DomainEvent;
import com.example.elibrary.domain.event.LoanEvents;
import com.example.elibrary.infrastructure.persistence.entity.OutboxEventEntity;
import com.example.elibrary.infrastructure.persistence.repository.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Stores domain events next to the aggregate they describe.
 *
 * <p>The write always happens inside the caller's transaction, therefore either both the state
 * change and its notification are visible, or neither is.
 */
@Slf4j
@Repository
public class OutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final OutboxJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public OutboxRepositoryAdapter(OutboxJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(DomainEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setEventId(event.eventId());
        entity.setEventType(event.eventType());
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateId(event.aggregateId());
        entity.setPayload(serialize(event));
        entity.setCreatedAt(event.occurredAt());
        jpa.save(entity);
    }

    @Override
    public List<DomainEvent> findUnpublished(int batchSize) {
        return jpa.findUnpublished(PageRequest.of(0, Math.max(batchSize, 1)))
                .stream()
                .map(this::deserialize)
                .toList();
    }

    @Override
    public void markPublished(List<String> eventIds) {
        jpa.markPublished(eventIds, java.time.Instant.now());
    }

    private DomainEvent deserialize(OutboxEventEntity entity) {
        Class<?> type = switch (entity.getEventType()) {
            case "loan.borrowed" -> LoanEvents.Borrowed.class;
            case "loan.returned" -> LoanEvents.Returned.class;
            case "loan.overdue" -> LoanEvents.Overdue.class;
            case "loan.renewed" -> LoanEvents.Renewed.class;
            default -> {
                log.warn("Unknown event type {} - skipping", entity.getEventType());
                yield null;
            }
        };
        if (type == null) {
            return new LoanEvents.Overdue(entity.getEventId(), entity.getEventType(),
                    entity.getAggregateType(), entity.getAggregateId(), entity.getCreatedAt(),
                    null, null, null);
        }
        try {
            return (DomainEvent) objectMapper.readValue(entity.getPayload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored event " + entity.getEventId() + " is not readable", e);
        }
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Event cannot be serialized", e);
        }
    }
}
