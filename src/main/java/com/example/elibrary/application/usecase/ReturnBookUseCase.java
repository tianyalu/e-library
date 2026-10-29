package com.example.elibrary.application.usecase;

import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.application.port.out.CopyRepositoryPort;
import com.example.elibrary.application.port.out.FineRepositoryPort;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.application.port.out.OutboxRepositoryPort;
import com.example.elibrary.domain.event.LoanEvents;
import com.example.elibrary.domain.exception.ResourceNotFoundException;
import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.policy.LoanPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * 还书。
 *
 * <p>归还做成幂等而不是冲突：同一条借阅重复还，返回同样的结果，第二次不报错。
 * 这样调用方超时重试后可以放心把"已归还"当成成功。
 */
@Slf4j
@Service
public class ReturnBookUseCase {

    public record Result(long loanId,
                         long bookId,
                         Instant returnedAt,
                         int daysOverdue,
                         String fineAmount,
                         boolean alreadyReturned) {
    }

    private final LoanRepositoryPort loans;
    private final CopyRepositoryPort copies;
    private final BookRepositoryPort books;
    private final FineRepositoryPort fines;
    private final OutboxRepositoryPort outbox;
    private final LoanPolicy policy;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public ReturnBookUseCase(LoanRepositoryPort loans,
                             CopyRepositoryPort copies,
                             BookRepositoryPort books,
                             FineRepositoryPort fines,
                             OutboxRepositoryPort outbox,
                             LoanPolicy policy,
                             Clock clock,
                             MeterRegistry meterRegistry) {
        this.loans = loans;
        this.copies = copies;
        this.books = books;
        this.fines = fines;
        this.outbox = outbox;
        this.policy = policy;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result execute(Long memberId, Long loanId) {
        Loan loan = loans.findByIdAndMemberId(loanId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.isReturned()) {
            meterRegistry.counter("elibrary.loan.return", "outcome", "already-returned").increment();
            return snapshotOf(loan, true);
        }

        Instant now = clock.instant();
        Optional<Fine> createdFine = loan.returnItem(now, policy);

        BookCopy copy = copies.findById(loan.getCopyId())
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", loan.getCopyId()));
        copy.takeBack();
        copies.save(copy);

        books.creditAvailableCopies(loan.getBookId());
        Loan saved = loans.save(loan);
        createdFine.ifPresent(fines::save);

        outbox.append(new LoanEvents.Returned(
                LoanEvents.newEventId(), "loan.returned", "Loan", String.valueOf(loan.getId()), now,
                loan.getMemberId(), loan.getBookId(), loan.getCopyId(), saved.getDaysOverdue()));

        meterRegistry.counter("elibrary.loan.return", "outcome", createdFine.isPresent() ? "late" : "on-time")
                .increment();
        log.info("Returned loanId={} memberId={} daysOverdue={} fine={}",
                loan.getId(), memberId, saved.getDaysOverdue(), createdFine.map(f -> f.getAmount().toString()).orElse("-"));

        return new Result(saved.getId(), saved.getBookId(), saved.getReturnedAt(), saved.getDaysOverdue(),
                createdFine.map(f -> f.getAmount().toString()).orElse(null), false);
    }

    private Result snapshotOf(Loan loan, boolean alreadyReturned) {
        String fineAmount = fines.findByLoanId(loan.getId()).map(f -> f.getAmount().toString()).orElse(null);
        return new Result(loan.getId(), loan.getBookId(), loan.getReturnedAt(), loan.getDaysOverdue(),
                fineAmount, alreadyReturned);
    }
}
