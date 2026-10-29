package com.example.elibrary.application.dto;

import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCategory;

/** Full catalogue item, including live availability. Safe to cache (no mutable state). */
public record BookDetail(Long id,
                         String isbn,
                         String title,
                         String author,
                         BookCategory category,
                         Integer publishedYear,
                         int totalCopies,
                         int availableCopies,
                         boolean borrowable) {

    public static BookDetail from(Book book) {
        return new BookDetail(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(), book.getCategory(),
                book.getPublishedYear(), book.getTotalCopies(), book.getAvailableCopies(), book.borrowable());
    }
}
