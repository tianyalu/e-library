package com.example.elibrary.infrastructure.persistence.entity;

import com.example.elibrary.domain.model.LoanStatus;
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

import java.time.Instant;

/**
 * Write model of a loan.
 *
 * <p>The {@code active_flag} generated column of the DDL is intentionally not mapped: it belongs
 * to the database (constraint enforcement), not to the domain.
 */
@Entity
@Table(name = "loan")
@Getter
@Setter
@NoArgsConstructor
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "copy_id", nullable = false)
    private Long copyId;

    @Column(name = "borrowed_at", nullable = false)
    private Instant borrowedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "renew_count", nullable = false)
    private int renewCount;

    @Column(name = "days_overdue", nullable = false)
    private int daysOverdue;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
