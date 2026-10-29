package com.example.elibrary.application.port.out;

import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.FineStatus;

import java.util.List;
import java.util.Optional;

/** Overdue penalties. */
public interface FineRepositoryPort {

    Fine save(Fine fine);

    Optional<Fine> findById(Long fineId);

    List<Fine> findByMemberAndStatus(Long memberId, FineStatus status);

    Optional<Fine> findByLoanId(Long loanId);
}
