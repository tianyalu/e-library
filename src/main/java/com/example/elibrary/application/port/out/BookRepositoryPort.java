package com.example.elibrary.application.port.out;

import com.example.elibrary.application.PagedResult;
import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCategory;

import java.util.List;
import java.util.Optional;

/** 书目与库存的持久化端口。 */
public interface BookRepositoryPort {

    Optional<Book> findById(Long id);

    List<Book> findByIds(List<Long> ids);

    Optional<Book> findByIsbn(String isbn);

    Book save(Book book);

    /**
     * 一条语句完成、原子的库存扣减：
     * {@code UPDATE book SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0}。
     *
     * @return true 表示这次抢到了；false 表示没余量了
     */
    boolean debitAvailableCopies(Long bookId);

    /** 归还时把一册加回库存。 */
    void creditAvailableCopies(Long bookId);

    PagedResult<Book> search(String keyword, BookCategory category, String author, int page, int size);

    /** 全站 {@code available_copies} 求和，对账用。 */
    long sumAvailableCopies();

    /** 全站 {@code total_copies} 求和，对账用。 */
    long sumTotalCopies();
}
