package com.example.elibrary.application.service;

import com.example.elibrary.application.dto.FineView;
import com.example.elibrary.application.port.out.FineRepositoryPort;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.domain.model.FineStatus;
import com.example.elibrary.domain.model.LoanStatus;
import com.example.elibrary.domain.money.Money;
import com.example.elibrary.domain.policy.LoanPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Everything a member needs to see about their own account: what they hold now, what they owe,
 * and how much room they still have.
 */
@Slf4j
@Service
public class MemberAccountService {

    private final LoanRepositoryPort loans;
    private final FineRepositoryPort fines;
    private final LoanPolicy policy;

    public MemberAccountService(LoanRepositoryPort loans, FineRepositoryPort fines, LoanPolicy policy) {
        this.loans = loans;
        this.fines = fines;
        this.policy = policy;
    }

    public record AccountSummary(long openLoans,
                                 long borrowingQuota,
                                 long remainingQuota,
                                 long outstandingFines,
                                 String totalFineAmount,
                                 boolean canBorrow) {

    }

    public List<FineView> outstandingFines(Long memberId) {
        return fines.findByMemberAndStatus(memberId, FineStatus.PENDING).stream().map(FineView::from).toList();
    }

    public AccountSummary summary(Long memberId) {
        long openLoans = loans.countOpenLoans(memberId);
        List<FineView> pending = outstandingFines(memberId);
        Money total = pending.isEmpty()
                ? Money.zero("CNY")
                : Money.of(pending.stream()
                        .map(f -> f.amount())
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add), pending.get(0).currency());
        long quota = policy.maxActiveLoans();
        return new AccountSummary(openLoans, quota, Math.max(quota - openLoans, 0L), pending.size(),
                total.toString(), pending.isEmpty() && openLoans < quota);
    }
}
