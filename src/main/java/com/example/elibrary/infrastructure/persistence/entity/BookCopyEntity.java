package com.example.elibrary.infrastructure.persistence.entity;

import com.example.elibrary.domain.model.CopyStatus;
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

/** Write model of one physical copy. */
@Entity
@Table(name = "book_copy")
@Getter
@Setter
@NoArgsConstructor
public class BookCopyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "barcode", nullable = false, unique = true, length = 32)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CopyStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
