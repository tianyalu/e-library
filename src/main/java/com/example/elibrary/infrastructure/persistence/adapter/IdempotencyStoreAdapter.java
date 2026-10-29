package com.example.elibrary.infrastructure.persistence.adapter;

import com.example.elibrary.application.port.out.IdempotencyStorePort;
import com.example.elibrary.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.example.elibrary.infrastructure.persistence.repository.IdempotencyJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
public class IdempotencyStoreAdapter implements IdempotencyStorePort {

    private final IdempotencyJpaRepository jpa;

    public IdempotencyStoreAdapter(IdempotencyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<IdempotencyRecord> find(Long memberId, String endpoint, String key) {
        return jpa.findByMemberIdAndEndpointAndIdempotencyKey(memberId, endpoint, key)
                .map(entity -> new IdempotencyRecord(entity.getMemberId(), entity.getEndpoint(),
                        entity.getIdempotencyKey(), entity.getRequestHash(), entity.getHttpStatus(),
                        entity.getResponseBody(), entity.getCreatedAt()));
    }

    /**
     * {@code false} means somebody else inserted the same key first - typically the client retrying
     * concurrently. The unique index does the coordination, no distributed lock required.
     */
    @Override
    public boolean trySave(IdempotencyRecord record) {
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.setMemberId(record.memberId());
        entity.setEndpoint(record.endpoint());
        entity.setIdempotencyKey(record.key());
        entity.setRequestHash(record.requestHash());
        entity.setHttpStatus(record.httpStatus());
        entity.setResponseBody(record.responseBody());
        entity.setCreatedAt(record.createdAt());
        try {
            jpa.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Concurrent idempotency insert detected for key={}", record.key());
            return false;
        }
    }
}
