package com.example.elibrary.application.usecase;

import com.example.elibrary.application.port.out.FineRepositoryPort;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.application.port.out.OutboxRepositoryPort;
import com.example.elibrary.domain.event.LoanEvents;
import com.example.elibrary.domain.exception.MembershipRestrictionException;
import com.example.elibrary.domain.exception.ResourceNotFoundException;
import com.example.elibrary.domain.model.FineStatus;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.policy.LoanPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * 续借。借阅仍然开着，只是把到期日往后推；次数用尽或者会员还欠着钱就拒绝。
 */
@Slf4j
@Service
public class RenewLoanUseCase {

    public record Result(long loanId, Instant previousDueAt, Instant newDueAt, int renewCount) {
    }

    private final LoanRepositoryPort loans;
    private final FineRepositoryPort fines;
    private final OutboxRepositoryPort outbox;
    private final LoanPolicy policy;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public RenewLoanUseCase(LoanRepositoryPort loans,
                            FineRepositoryPort fines,
                            OutboxRepositoryPort outbox,
                            LoanPolicy policy,
                            Clock clock,
                            MeterRegistry meterRegistry) {
        this.loans = loans;
        this.fines = fines;
        this.outbox = outbox;
        this.policy = policy;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result execute(Long memberId, Long loanId) {
        Instant now = clock.instant();
        Loan loan = loans.findByIdAndMemberId(loanId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (!fines.findByMemberAndStatus(memberId, FineStatus.PENDING).isEmpty()) {
            meterRegistry.counter("elibrary.loan.renew", "outcome", "unpaid-fine").increment();
            throw MembershipRestrictionException.renewalNotAllowed(loanId, "unpaid penalty outstanding");
        }

        Instant previousDueAt = loan.getDueAt();
        loan.renew(now, policy);
        Loan saved = loans.save(loan);

        outbox.append(new LoanEvents.Renewed(
                LoanEvents.newEventId(), "loan.renewed", "Loan", String.valueOf(loanId), now,
                memberId, saved.getBookId(), previousDueAt, saved.getDueAt(), saved.getRenewCount()));

        meterRegistry.counter("elibrary.loan.renew", "outcome", "success").increment();
        log.info("Renewed loanId={} memberId={} from {} to {} (renewal #{})",
                loanId, memberId, previousDueAt, saved.getDueAt(), saved.getRenewCount());
        return new Result(loanId, previousDueAt, saved.getDueAt(), saved.getRenewCount());
    }
}
