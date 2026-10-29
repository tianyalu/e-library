package com.example.elibrary.application;

import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.application.port.out.CopyRepositoryPort;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.application.port.out.MemberRepositoryPort;
import com.example.elibrary.application.service.ReconciliationService;
import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCategory;
import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.LoanStatus;
import com.example.elibrary.domain.model.Member;
import com.example.elibrary.domain.model.MemberRole;
import com.example.elibrary.domain.exception.BusinessErrorCode;
import com.example.elibrary.domain.exception.BusinessException;
import com.example.elibrary.application.usecase.BorrowBookUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 这个测试是真的在验并发，而不是嘴上说"支持高并发"。
 *
 * <p>32 个会员去抢同一本书的唯一一本余量。无论怎么交错执行，都只能有一个赢家，
 * 库存最后停在 0，并且两种口径（计数器和实物副本行）事后必须一致。
 */
@SpringBootTest
@ActiveProfiles("test")
class BorrowConcurrencyTest {

    private static final int THREADS = 32;

    @Autowired
    private BorrowBookUseCase borrowBookUseCase;
    @Autowired
    private BookRepositoryPort books;
    @Autowired
    private CopyRepositoryPort copies;
    @Autowired
    private MemberRepositoryPort members;
    @Autowired
    private LoanRepositoryPort loans;
    @Autowired
    private ReconciliationService reconciliationService;

    @Test
    @DisplayName("抢最后一本书：只有一个赢家，不超卖，账实不漂移")
    void onlyOneWinnerForTheLastCopy() throws Exception {
        Book book = books.save(new Book("978-0000000001", "Single Copy Book", "Concurrent Author",
                BookCategory.TECHNOLOGY, 2026));
        copies.save(new BookCopy(book.getId(), "BC-SINGLE-1"));
        book.registerCopy();
        books.save(book);

        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            memberIds.add(members.save(new Member("Member " + i, "member" + i + "@test.local", "hash",
                    MemberRole.MEMBER)).getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(THREADS, 16));
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<String>> outcomes = new ArrayList<>();
        for (Long memberId : memberIds) {
            outcomes.add(pool.submit(() -> {
                startGate.await();
                try {
                    borrowBookUseCase.execute(memberId, book.getId());
                    return "SUCCESS";
                } catch (BusinessException e) {
                    return e.code().name();
                } catch (Exception e) {
                    return "UNEXPECTED:" + e.getClass().getSimpleName();
                }
            }));
        }
        startGate.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        List<String> results = outcomes.stream().map(BorrowConcurrencyTest::get).toList();
        long winners = results.stream().filter("SUCCESS"::equals).count();
        long noStock = results.stream().filter(BusinessErrorCode.COPY_NOT_AVAILABLE.name()::equals).count();

        assertThat(results).doesNotContain("UNEXPECTED");
        assertThat(winners).as("exactly one caller may take the last copy").isEqualTo(1);
        assertThat(noStock).isEqualTo(THREADS - 1);

        assertThat(books.findById(book.getId()).orElseThrow().getAvailableCopies()).isZero();
        assertThat(loans.countOpenLoans(memberIds.get(0))
                + IntStream.range(1, THREADS).mapToLong(i -> loans.countOpenLoans(memberIds.get(i))).sum())
                .isEqualTo(1);
        assertThat(copies.countByBookIdAndStatus(book.getId(), com.example.elibrary.domain.model.CopyStatus.ON_LOAN))
                .isEqualTo(1);
        assertThat(loans.countByStatus(LoanStatus.ACTIVE)).isEqualTo(1);

        ReconciliationService.Report report = reconciliationService.check();
        assertThat(report.findings()).as("ledger and physical stock must still agree").isEmpty();
        assertThat(report.consistent()).isTrue();
    }

    private static String get(Future<String> future) {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
