package com.example.elibrary.infrastructure.persistence.entity;

import com.example.elibrary.domain.model.FineStatus;
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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Write model of a fine. Money is stored as amount + currency - two primitive columns, never as
 * one float column.
 */
@Entity
@Table(name = "fine")
@Getter
@Setter
@NoArgsConstructor
public class FineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false, unique = true)
    private Long loanId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "days_overdue", nullable = false)
    private int daysOverdue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FineStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
