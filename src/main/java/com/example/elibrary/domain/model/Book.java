package com.example.elibrary.domain.model;

import lombok.Getter;

/**
 * 书目（一个书名），不是一本实物。
 *
 * <p>{@code availableCopies} 是防超卖的那把尺子：真正的扣减在持久层用带条件的 UPDATE 做，
 * 这里只在内存里维持 {@code 0 <= 可借数 <= 馆藏数} 不被写坏。
 */
@Getter
public class Book {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private BookCategory category;
    private Integer publishedYear;
    private int totalCopies;
    private int availableCopies;
    private Long version;

    private Book() {
    }

    public Book(String isbn, String title, String author, BookCategory category, Integer publishedYear) {
        this(null, isbn, title, author, category, publishedYear, 0, 0, null);
    }

    private Book(Long id, String isbn, String title, String author, BookCategory category, Integer publishedYear,
                 int totalCopies, int availableCopies, Long version) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publishedYear = publishedYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.version = version;
    }

    /** 只有持久层用它把已有行还原成对象；业务侧用公开构造函数新建。 */
    public static Book restore(Long id, String isbn, String title, String author, BookCategory category,
                               Integer publishedYear, int totalCopies, int availableCopies, Long version) {
        return new Book(id, isbn, title, author, category, publishedYear, totalCopies, availableCopies, version);
    }

    /** 让持久层把自增 ID 回填进来。 */
    public void assignIdForNewPersistence(Long generatedId) {
        this.id = generatedId;
    }

    /** 新入藏一本实物。 */
    public void registerCopy() {
        totalCopies++;
        availableCopies++;
    }

    /** 下架一本借出。计数器不会变负。 */
    public void takeForLending() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No copy available for book " + id);
        }
        availableCopies--;
    }

    /** 归还后上架一本。可借数不会超过馆藏数。 */
    public void releaseAfterReturn() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("Inventory overflow for book " + id);
        }
        availableCopies++;
    }

    public boolean borrowable() {
        return availableCopies > 0;
    }
}
