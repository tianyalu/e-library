package com.example.elibrary.infrastructure.persistence.adapter;

import com.example.elibrary.application.PagedResult;
import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCategory;
import com.example.elibrary.infrastructure.persistence.entity.BookEntity;
import com.example.elibrary.infrastructure.persistence.mapper.PersistenceMapper;
import com.example.elibrary.infrastructure.persistence.repository.BookJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BookRepositoryAdapter implements BookRepositoryPort {

    private final BookJpaRepository jpa;

    public BookRepositoryAdapter(BookJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Book> findById(Long id) {
        return jpa.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Book> findByIds(List<Long> ids) {
        return jpa.findAllById(ids).stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return jpa.findByIsbn(isbn).map(PersistenceMapper::toDomain);
    }

    @Override
    public Book save(Book book) {
        BookEntity entity = book.getId() == null
                ? new BookEntity()
                : jpa.findById(book.getId()).orElseGet(BookEntity::new);
        PersistenceMapper.apply(book, entity);
        BookEntity saved = jpa.saveAndFlush(entity);
        book.assignIdForNewPersistence(saved.getId());
        return PersistenceMapper.toDomain(saved);
    }

    @Override
    public boolean debitAvailableCopies(Long bookId) {
        return jpa.debitAvailableCopies(bookId) == 1;
    }

    @Override
    public void creditAvailableCopies(Long bookId) {
        jpa.creditAvailableCopies(bookId);
    }

    @Override
    public PagedResult<Book> search(String keyword, BookCategory category, String author, int page, int size) {
        Page<BookEntity> result = jpa.search(keyword, category, author,
                PageRequest.of(page, Math.max(size, 1), Sort.by(Sort.Direction.ASC, "title")));
        return PagedResult.of(result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public long sumAvailableCopies() {
        return orZero(jpa.sumAvailableCopies());
    }

    @Override
    public long sumTotalCopies() {
        return orZero(jpa.sumTotalCopies());
    }

    private long orZero(Long value) {
        return value == null ? 0L : value;
    }
}
