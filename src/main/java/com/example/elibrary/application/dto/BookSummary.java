package com.example.elibrary.application.dto;

import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCategory;

/** Immutable catalogue row returned by search endpoints (also the cached shape). */
public record BookSummary(Long id,
                          String isbn,
                          String title,
                          String author,
                          BookCategory category,
                          Integer publishedYear,
                          int totalCopies,
                          int availableCopies,
                          boolean borrowable) {

    public static BookSummary from(Book book) {
        return new BookSummary(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(), book.getCategory(),
                book.getPublishedYear(), book.getTotalCopies(), book.getAvailableCopies(), book.borrowable());
    }
}
