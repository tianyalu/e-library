package com.example.elibrary.infrastructure.persistence.repository;

import com.example.elibrary.domain.model.BookCategory;
import com.example.elibrary.infrastructure.persistence.entity.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookJpaRepository extends JpaRepository<BookEntity, Long> {

    Optional<BookEntity> findByIsbn(String isbn);

    /**
     * 防超卖的闸门就是这一条语句：MySQL 在这行上串行写者并原子地判断
     * {@code availableCopies > 0}，所以 N 本余量并发请求里恰好 N 个成功。
     *
     * <p>{@code clearAutomatically} 是为了避免同一事务里后续读到更新前的快照。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BookEntity b set b.availableCopies = b.availableCopies - 1 "
            + "where b.id = :id and b.availableCopies > 0")
    int debitAvailableCopies(@Param("id") Long id);

    /** 上面那条 UPDATE 的反向操作，用馆藏数兜住，账上永远不会被加过头。 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BookEntity b set b.availableCopies = b.availableCopies + 1 "
            + "where b.id = :id and b.availableCopies < b.totalCopies")
    int creditAvailableCopies(@Param("id") Long id);

    /** 可选过滤条件没传时退化成 {@code TRUE}：一条预编译语句搞定，不用拼动态 SQL。 */
    @Query("""
            select b from BookEntity b
             where (:keyword is null
                    or lower(b.title) like concat('%', :keyword, '%')
                    or lower(b.author) like concat('%', :keyword, '%')
                    or lower(b.isbn) like concat('%', :keyword, '%'))
               and (:category is null or b.category = :category)
               and (:author is null or lower(b.author) like concat('%', :author, '%'))
            """)
    Page<BookEntity> search(@Param("keyword") String keyword,
                            @Param("category") BookCategory category,
                            @Param("author") String author,
                            Pageable pageable);

    @Query("select sum(b.availableCopies) from BookEntity b")
    Long sumAvailableCopies();

    @Query("select sum(b.totalCopies) from BookEntity b")
    Long sumTotalCopies();
}
