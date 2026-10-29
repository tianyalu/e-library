package com.example.elibrary.infrastructure.persistence.mapper;

import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.Loan;
import com.example.elibrary.domain.model.Member;
import com.example.elibrary.domain.money.Money;
import com.example.elibrary.infrastructure.persistence.entity.BookCopyEntity;
import com.example.elibrary.infrastructure.persistence.entity.BookEntity;
import com.example.elibrary.infrastructure.persistence.entity.FineEntity;
import com.example.elibrary.infrastructure.persistence.entity.LoanEntity;
import com.example.elibrary.infrastructure.persistence.entity.MemberEntity;

/**
 * 领域对象和持久层模型之间的转换。
 *
 * <p>集中在一处，是为了让 domain 不沾 ORM：注解不会蔓延进业务代码，
 * 将来换存储只改这个类和适配层。
 */
public final class PersistenceMapper {

    private PersistenceMapper() {
    }

    // ---------------- Book ----------------

    public static Book toDomain(BookEntity entity) {
        return Book.restore(entity.getId(), entity.getIsbn(), entity.getTitle(), entity.getAuthor(),
                entity.getCategory(), entity.getPublishedYear(), entity.getTotalCopies(),
                entity.getAvailableCopies(), entity.getVersion());
    }

    public static void apply(Book domain, BookEntity entity) {
        entity.setIsbn(domain.getIsbn());
        entity.setTitle(domain.getTitle());
        entity.setAuthor(domain.getAuthor());
        entity.setCategory(domain.getCategory());
        entity.setPublishedYear(domain.getPublishedYear());
        entity.setTotalCopies(domain.getTotalCopies());
        entity.setAvailableCopies(domain.getAvailableCopies());
    }

    // ---------------- BookCopy ----------------

    public static BookCopy toDomain(BookCopyEntity entity) {
        return BookCopy.restore(entity.getId(), entity.getBookId(), entity.getBarcode(), entity.getStatus(),
                entity.getVersion());
    }

    public static void apply(BookCopy domain, BookCopyEntity entity) {
        entity.setBookId(domain.getBookId());
        entity.setBarcode(domain.getBarcode());
        entity.setStatus(domain.getStatus());
    }

    // ---------------- Member ----------------

    public static Member toDomain(MemberEntity entity) {
        return Member.restore(entity.getId(), entity.getName(), entity.getEmail(), entity.getPasswordHash(),
                entity.getRole(), entity.getStatus(), entity.getVersion());
    }

    public static void apply(Member domain, MemberEntity entity) {
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(domain.getRole());
        entity.setStatus(domain.getStatus());
    }

    // ---------------- Loan ----------------

    public static Loan toDomain(LoanEntity entity) {
        return Loan.restore(entity.getId(), entity.getMemberId(), entity.getBookId(), entity.getCopyId(),
                entity.getBorrowedAt(), entity.getDueAt(), entity.getReturnedAt(), entity.getStatus(),
                entity.getRenewCount(), entity.getDaysOverdue(), entity.getVersion());
    }

    public static void apply(Loan domain, LoanEntity entity) {
        entity.setMemberId(domain.getMemberId());
        entity.setBookId(domain.getBookId());
        entity.setCopyId(domain.getCopyId());
        entity.setBorrowedAt(domain.getBorrowedAt());
        entity.setDueAt(domain.getDueAt());
        entity.setReturnedAt(domain.getReturnedAt());
        entity.setStatus(domain.getStatus());
        entity.setRenewCount(domain.getRenewCount());
        entity.setDaysOverdue(domain.getDaysOverdue());
    }

    // ---------------- Fine ----------------

    public static Fine toDomain(FineEntity entity) {
        return Fine.restore(entity.getId(), entity.getLoanId(), entity.getMemberId(),
                Money.of(entity.getAmount(), entity.getCurrency()), entity.getDaysOverdue(),
                entity.getStatus(), entity.getVersion());
    }

    public static void apply(Fine domain, FineEntity entity) {
        entity.setLoanId(domain.getLoanId());
        entity.setMemberId(domain.getMemberId());
        entity.setAmount(domain.getAmount().amount());
        entity.setCurrency(domain.getAmount().currency());
        entity.setDaysOverdue(domain.getDaysOverdue());
        entity.setStatus(domain.getStatus());
    }
}
