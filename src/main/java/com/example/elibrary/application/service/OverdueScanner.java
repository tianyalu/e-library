package com.example.elibrary.application.service;

import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.application.port.out.OutboxRepositoryPort;
import com.example.elibrary.domain.event.LoanEvents;
import com.example.elibrary.domain.model.Loan;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 把到期的借阅从 ACTIVE 挪到 OVERDUE。
 *
 * <p>扫描是幂等的（重跑没有副作用）并且分块，所以事务都很短，不会锁整张表。
 * 规模更大的时候会换成分片扫描或 CDC，但原则不变：状态从数据推出来，
 * 不依赖"定时器恰好触发一次"这种假设。
 */
@Slf4j
@Component
public class OverdueScanner {

    private static final int BATCH_SIZE = 500;

    private final LoanRepositoryPort loans;
    private final OutboxRepositoryPort outbox;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public OverdueScanner(LoanRepositoryPort loans, OutboxRepositoryPort outbox, Clock clock,
                          MeterRegistry meterRegistry) {
        this.loans = loans;
        this.outbox = outbox;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${elibrary.scanner.overdue-delay-millis:300000}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scan() {
        Instant now = clock.instant();
        List<Loan> expired = loans.findActivePastDue(now, BATCH_SIZE);
        int marked = 0;
        for (Loan loan : expired) {
            if (loan.markOverdueIfNeeded(now)) {
                loans.save(loan);
                outbox.append(new LoanEvents.Overdue(LoanEvents.newEventId(), "loan.overdue", "Loan",
                        String.valueOf(loan.getId()), now, loan.getMemberId(), loan.getBookId(), loan.getDueAt()));
                marked++;
            }
        }
        meterRegistry.counter("elibrary.scanner.overdue.marked").increment(marked);
        if (marked > 0) {
            log.info("Overdue scanner transitioned {} loan(s) to OVERDUE", marked);
        }
    }
}
