package com.example.elibrary.infrastructure.persistence.repository;

import com.example.elibrary.domain.model.CopyStatus;
import com.example.elibrary.infrastructure.persistence.entity.BookCopyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookCopyJpaRepository extends JpaRepository<BookCopyEntity, Long> {

    /**
     * Takes the next available copy and locks it for the rest of the transaction.
     *
     * <p>Native SQL because the lock modifier must survive with a {@code LIMIT 1}; the statement
     * is valid for MySQL and H2 alike ({@code FOR UPDATE} is the last clause in both dialects).
     */
    @Query(value = "select * from book_copy where book_id = :bookId and status = 'AVAILABLE' "
            + "order by id limit 1 for update", nativeQuery = true)
    Optional<BookCopyEntity> findFirstAvailableForUpdate(@Param("bookId") Long bookId);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByStatus(CopyStatus status);
}
