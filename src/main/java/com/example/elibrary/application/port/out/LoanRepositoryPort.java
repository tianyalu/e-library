package com.example.elibrary.application.port.out;

import com.example.elibrary.application.PagedResult;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.model.LoanStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 借阅记录（借阅台账）。 */
public interface LoanRepositoryPort {

    Loan save(Loan loan);

    Optional<Loan> findById(Long id);

    /** 带归属判断的查询，防止读到别人的借阅记录。 */
    Optional<Loan> findByIdAndMemberId(Long id, Long memberId);

    boolean existsOpenLoan(Long memberId, Long bookId);

    long countOpenLoans(Long memberId);

    PagedResult<Loan> findByMember(Long memberId, LoanStatus status, int page, int size);

    PagedResult<Loan> findAll(LoanStatus status, int page, int size);

    /** 给逾期扫描分批用，按 {@code dueAt} 往前推进。 */
    List<Loan> findActivePastDue(Instant dueBefore, int limit);

    long countByStatus(LoanStatus status);

    long count();
}
