package com.example.elibrary.infrastructure.persistence.adapter;

import com.example.elibrary.application.port.out.CopyRepositoryPort;
import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.CopyStatus;
import com.example.elibrary.infrastructure.persistence.entity.BookCopyEntity;
import com.example.elibrary.infrastructure.persistence.mapper.PersistenceMapper;
import com.example.elibrary.infrastructure.persistence.repository.BookCopyJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CopyRepositoryAdapter implements CopyRepositoryPort {

    private final BookCopyJpaRepository jpa;

    public CopyRepositoryAdapter(BookCopyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<BookCopy> findFirstAvailableForUpdate(Long bookId) {
        return jpa.findFirstAvailableForUpdate(bookId).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<BookCopy> findById(Long id) {
        return jpa.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public BookCopy save(BookCopy copy) {
        BookCopyEntity entity = copy.getId() == null
                ? new BookCopyEntity()
                : jpa.findById(copy.getId()).orElseGet(BookCopyEntity::new);
        PersistenceMapper.apply(copy, entity);
        BookCopyEntity saved = jpa.saveAndFlush(entity);
        copy.assignIdForNewPersistence(saved.getId());
        return PersistenceMapper.toDomain(saved);
    }

    @Override
    public void saveAll(List<BookCopy> copies) {
        copies.forEach(this::save);
    }

    @Override
    public long countByBookIdAndStatus(Long bookId, CopyStatus status) {
        return jpa.countByBookIdAndStatus(bookId, status);
    }

    @Override
    public long countByStatus(CopyStatus status) {
        return jpa.countByStatus(status);
    }
}
