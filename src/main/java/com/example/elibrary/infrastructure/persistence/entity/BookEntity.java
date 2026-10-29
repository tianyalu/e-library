package com.example.elibrary.infrastructure.persistence.entity;

import com.example.elibrary.domain.model.BookCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 书目的写模型。
 *
 * <p>{@code availableCopies} 是热点字段：不做任何缓存，只由 {@code BookRepositoryAdapter}
 * 里那条带条件的 UPDATE 改，所以这行永远卖不超。
 */
@Entity
@Table(name = "book")
@Getter
@Setter
@NoArgsConstructor
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "isbn", nullable = false, unique = true, length = 32)
    private String isbn;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "author", nullable = false, length = 180)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private BookCategory category;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    @Column(name = "available_copies", nullable = false)
    private int availableCopies;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
