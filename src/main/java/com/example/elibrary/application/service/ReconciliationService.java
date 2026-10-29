package com.example.elibrary.application.service;

import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.application.port.out.CopyRepositoryPort;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.domain.model.CopyStatus;
import com.example.elibrary.domain.model.LoanStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 只读检查：把"有几本在外面"的两种口径对一遍。
 *
 * <p>两套账是故意分开记的：
 * <ol>
 *   <li><b>快账</b> {@code book.available_copies}， BorrowBookUseCase 在事务里靠它判断有没有余量；</li>
 *   <li><b>实物账</b> {@code book_copy.status} 行，可追溯，出问题以它为准。</li>
 * </ol>
 * 两边一旦对不上，说明中间出了岔子（部分失败、手工改过库、代码 bug），得让运维知道，
 * 不能等到读者借不到书才发现。
 */
@Slf4j
@Service
public class ReconciliationService {

    private final BookRepositoryPort books;
    private final CopyRepositoryPort copies;
    private final LoanRepositoryPort loans;
    private final Clock clock;

    public ReconciliationService(BookRepositoryPort books, CopyRepositoryPort copies,
                                 LoanRepositoryPort loans, Clock clock) {
        this.books = books;
        this.copies = copies;
        this.loans = loans;
        this.clock = clock;
    }

    public record Finding(String check, String message) {
    }

    public record Report(Instant checkedAt,
                         long physicalAvailable,
                         long ledgerAvailable,
                         long openLoans,
                         long copiesOnLoan,
                         boolean consistent,
                         List<Finding> findings,
                         long elapsedMillis) {
    }

    public Report check() {
        long started = System.nanoTime();
        Instant checkedAt = clock.instant();
        List<Finding> findings = new ArrayList<>();

        long physicalAvailable = copies.countByStatus(CopyStatus.AVAILABLE);
        long physicalMaintenance = copies.countByStatus(CopyStatus.MAINTENANCE);
        long physicalOnLoan = copies.countByStatus(CopyStatus.ON_LOAN);
        long ledgerAvailable = books.sumAvailableCopies();
        if (physicalAvailable != ledgerAvailable) {
            findings.add(new Finding("INVENTORY_DRIFT",
                    "book.available_copies=" + ledgerAvailable + " but physically available copies="
                            + physicalAvailable));
        }

        long openLoans = loans.countByStatus(LoanStatus.ACTIVE) + loans.countByStatus(LoanStatus.OVERDUE);
        long copiesOnLoan = physicalOnLoan;
        if (openLoans != copiesOnLoan) {
            findings.add(new Finding("LOAN_DRIFT",
                    "open loans=" + openLoans + " but copies in ON_LOAN state=" + copiesOnLoan));
        }

        long physicalTotal = physicalAvailable + copiesOnLoan + physicalMaintenance;
        long ledgerTotal = books.sumTotalCopies();
        if (physicalTotal != ledgerTotal) {
            findings.add(new Finding("STOCK_DRIFT",
                    "book.total_copies=" + ledgerTotal + " but physical copies=" + physicalTotal));
        }

        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        if (!findings.isEmpty()) {
            log.warn("Reconciliation found {} issue(s): {}", findings.size(), findings);
        }
        return new Report(checkedAt, physicalAvailable, ledgerAvailable, openLoans, copiesOnLoan,
                findings.isEmpty(), findings, elapsedMillis);
    }
}
