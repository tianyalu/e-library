package com.example.elibrary.infrastructure.persistence.repository;

import com.example.elibrary.infrastructure.persistence.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyRecordEntity, Long> {

    Optional<IdempotencyRecordEntity> findByMemberIdAndEndpointAndIdempotencyKey(Long memberId, String endpoint,
                                                                                String idempotencyKey);
}
