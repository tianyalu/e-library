package com.example.elibrary.application.port.out;

import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.CopyStatus;

import java.util.List;
import java.util.Optional;

/** 实物副本。 */
public interface CopyRepositoryPort {

    /**
     * 挑一册可用的并锁住这一行（{@code SELECT ... FOR UPDATE}），
     * 锁会一直持有到外层事务结束。
     */
    Optional<BookCopy> findFirstAvailableForUpdate(Long bookId);

    Optional<BookCopy> findById(Long id);

    BookCopy save(BookCopy copy);

    void saveAll(List<BookCopy> copies);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    /** 按状态统计全站数量，对账时拿它跟快账比。 */
    long countByStatus(CopyStatus status);
}
