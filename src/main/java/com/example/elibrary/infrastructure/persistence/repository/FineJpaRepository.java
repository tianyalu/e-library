package com.example.elibrary.infrastructure.persistence.repository;

import com.example.elibrary.domain.model.FineStatus;
import com.example.elibrary.infrastructure.persistence.entity.FineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FineJpaRepository extends JpaRepository<FineEntity, Long> {

    List<FineEntity> findByMemberIdAndStatus(Long memberId, FineStatus status);

    Optional<FineEntity> findByLoanId(Long loanId);
}
