package com.example.elibrary.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Result snapshot keyed by (member, endpoint, client key).
 * The UNIQUE constraint on those three columns is what gives "exactly once" behaviour.
 */
@Entity
@Table(name = "idempotency_record")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "endpoint", nullable = false, length = 80)
    private String endpoint;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
