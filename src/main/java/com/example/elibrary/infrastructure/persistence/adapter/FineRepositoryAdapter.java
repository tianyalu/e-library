package com.example.elibrary.infrastructure.persistence.adapter;

import com.example.elibrary.application.port.out.FineRepositoryPort;
import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.FineStatus;
import com.example.elibrary.infrastructure.persistence.entity.FineEntity;
import com.example.elibrary.infrastructure.persistence.mapper.PersistenceMapper;
import com.example.elibrary.infrastructure.persistence.repository.FineJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class FineRepositoryAdapter implements FineRepositoryPort {

    private final FineJpaRepository jpa;

    public FineRepositoryAdapter(FineJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Fine save(Fine fine) {
        FineEntity entity = fine.getId() == null
                ? new FineEntity()
                : jpa.findById(fine.getId()).orElseGet(FineEntity::new);
        PersistenceMapper.apply(fine, entity);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        if (entity.getPaidAt() == null && fine.getStatus() != FineStatus.PENDING) {
            entity.setPaidAt(Instant.now());
        }
        FineEntity saved = jpa.saveAndFlush(entity);
        fine.assignIdForNewPersistence(saved.getId());
        return PersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Fine> findById(Long fineId) {
        return jpa.findById(fineId).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Fine> findByMemberAndStatus(Long memberId, FineStatus status) {
        return jpa.findByMemberIdAndStatus(memberId, status).stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Fine> findByLoanId(Long loanId) {
        return jpa.findByLoanId(loanId).map(PersistenceMapper::toDomain);
    }
}
