package com.example.elibrary.infrastructure.persistence.repository;

import com.example.elibrary.domain.model.LoanStatus;
import com.example.elibrary.infrastructure.persistence.entity.LoanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanJpaRepository extends JpaRepository<LoanEntity, Long> {

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<LoanStatus> statuses);

    long countByMemberIdAndStatusIn(Long memberId, Collection<LoanStatus> statuses);

    Optional<LoanEntity> findByIdAndMemberId(Long id, Long memberId);

    Page<LoanEntity> findByMemberId(Long memberId, Pageable pageable);

    Page<LoanEntity> findByMemberIdAndStatus(Long memberId, LoanStatus status, Pageable pageable);

    Page<LoanEntity> findByStatus(LoanStatus status, Pageable pageable);

    /**
     * Scanner batch. Only {@code ACTIVE} rows can be transitioned to OVERDUE, which makes the
     * scan idempotent: rows already moved are never selected again.
     */
    @Query("select l from LoanEntity l where l.status = :status and l.dueAt <= :threshold "
            + "order by l.dueAt asc")
    List<LoanEntity> findActivePastDue(@Param("status") LoanStatus status,
                                       @Param("threshold") Instant threshold,
                                       Pageable pageable);

    long countByStatus(LoanStatus status);
}
