package com.example.elibrary.application.service;

import com.example.elibrary.application.PagedResult;
import com.example.elibrary.application.dto.LoanView;
import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.application.port.out.LoanRepositoryPort;
import com.example.elibrary.domain.exception.ResourceNotFoundException;
import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.model.LoanStatus;
import com.example.elibrary.domain.policy.LoanPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 借阅记录的读侧。
 *
 * <p>查询一律带上归属判断：会员只能取到自己 {@code memberId} 的行——纵深防御，
 * 不单靠调用方的角色。
 *
 * <p>在借记录的逾期天数是实时算的，因为库里那列要等书还回来才有意义。
 */
@Slf4j
@Service
public class LoanQueryService {

    private final LoanRepositoryPort loans;
    private final BookRepositoryPort books;
    private final LoanPolicy policy;
    private final Clock clock;

    public LoanQueryService(LoanRepositoryPort loans, BookRepositoryPort books, LoanPolicy policy, Clock clock) {
        this.loans = loans;
        this.books = books;
        this.policy = policy;
        this.clock = clock;
    }

    public PagedResult<LoanView> myLoans(Long memberId, LoanStatus status, int page, int size) {
        return toViews(loans.findByMember(memberId, status, page, size));
    }

    public PagedResult<LoanView> allLoans(LoanStatus status, int page, int size) {
        return toViews(loans.findAll(status, page, size));
    }

    public LoanView findById(Long memberId, Long loanId, boolean librarian) {
        Loan loan = librarian
                ? loans.findById(loanId).orElseThrow(() -> new ResourceNotFoundException("Loan", loanId))
                : loans.findByIdAndMemberId(loanId, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        return toView(loan, bookTitles(List.of(loan.getBookId())));
    }

    /** 一次性把书名捞出来，避免经典的 N+1：一页一次查询，而不是一行一次。 */
    private PagedResult<LoanView> toViews(PagedResult<Loan> result) {
        List<Loan> content = result.content();
        if (content.isEmpty()) {
            return PagedResult.of(List.<LoanView>of(), result.page(), result.size(), result.totalElements());
        }
        Map<Long, String> titles = bookTitles(content.stream()
                .map(Loan::getBookId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<LoanView> views = content.stream().map(loan -> toView(loan, titles)).toList();
        return PagedResult.of(views, result.page(), result.size(), result.totalElements());
    }

    private Map<Long, String> bookTitles(List<Long> ids) {
        List<Long> bookIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return books.findByIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Book::getTitle, (a, b) -> a));
    }

    private LoanView toView(Loan loan, Map<Long, String> titles) {
        Instant now = clock.instant();
        boolean overdue = loan.isOpen() && policy.isOverdue(now, loan.getDueAt());
        long accruedDays = overdue ? policy.overdueDays(loan.getDueAt(), now) : 0L;
        return new LoanView(
                loan.getId(),
                loan.getBookId(),
                titles.getOrDefault(loan.getBookId(), "unknown"),
                loan.getCopyId(),
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt(),
                loan.getStatus(),
                overdue,
                loan.isOpen() ? accruedDays : loan.getDaysOverdue(),
                loan.getRenewCount());
    }
}
