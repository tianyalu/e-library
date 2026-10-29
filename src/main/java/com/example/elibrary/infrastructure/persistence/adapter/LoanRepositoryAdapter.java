package com.example.elibrary.infrastructure.persistence.adapter;

import com.example.elibrary.application.PagedResult;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.model.LoanStatus;
import com.example.elibrary.infrastructure.persistence.entity.LoanEntity;
import com.example.elibrary.infrastructure.persistence.mapper.PersistenceMapper;
import com.example.elibrary.infrastructure.persistence.repository.LoanJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanRepositoryAdapter implements LoanRepositoryPort {

    private final LoanJpaRepository jpa;

    public LoanRepositoryAdapter(LoanJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Loan save(Loan loan) {
        LoanEntity entity = loan.getId() == null
                ? new LoanEntity()
                : jpa.findById(loan.getId()).orElseGet(LoanEntity::new);
        PersistenceMapper.apply(loan, entity);
        LoanEntity saved = jpa.saveAndFlush(entity);
        loan.assignIdForNewPersistence(saved.getId());
        return PersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return jpa.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Loan> findByIdAndMemberId(Long id, Long memberId) {
        return jpa.findByIdAndMemberId(id, memberId).map(PersistenceMapper::toDomain);
    }

    @Override
    public boolean existsOpenLoan(Long memberId, Long bookId) {
        return jpa.existsByMemberIdAndBookIdAndStatusIn(memberId, bookId, LoanStatus.OPEN_STATUSES);
    }

    @Override
    public long countOpenLoans(Long memberId) {
        return jpa.countByMemberIdAndStatusIn(memberId, LoanStatus.OPEN_STATUSES);
    }

    @Override
    public PagedResult<Loan> findByMember(Long memberId, LoanStatus status, int page, int size) {
        Page<LoanEntity> result = status == null
                ? jpa.findByMemberId(memberId, pageable(page, size))
                : jpa.findByMemberIdAndStatus(memberId, status, pageable(page, size));
        return toPagedResult(result);
    }

    @Override
    public PagedResult<Loan> findAll(LoanStatus status, int page, int size) {
        Page<LoanEntity> result = status == null
                ? jpa.findAll(pageable(page, size))
                : jpa.findByStatus(status, pageable(page, size));
        return toPagedResult(result);
    }

    @Override
    public List<Loan> findActivePastDue(Instant dueBefore, int limit) {
        return jpa.findActivePastDue(LoanStatus.ACTIVE, dueBefore,
                        PageRequest.of(0, Math.max(limit, 1), Sort.by(Sort.Direction.ASC, "dueAt")))
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countByStatus(LoanStatus status) {
        return jpa.countByStatus(status);
    }

    @Override
    public long count() {
        return jpa.count();
    }

    private PagedResult<Loan> toPagedResult(Page<LoanEntity> page) {
        return PagedResult.of(page.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    /** Newest first - members care about what they borrowed last, excerpt when paging history. */
    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, Math.max(size, 1), Sort.by(Sort.Direction.DESC, "borrowedAt"));
    }
}
