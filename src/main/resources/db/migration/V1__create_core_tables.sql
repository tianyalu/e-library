-- ============================================================================
-- V1: 核心借阅表（MySQL 8）
--
-- book.available_copies 是热点计数器，扣减走 UPDATE ... SET c = c - 1 WHERE c > 0，
--   靠这条语句本身的原子性防超卖，不需要锁整张表。
-- loan.active_flag 是虚列（VIRTUAL）：已归还的行算出 NULL，配合
--   UNIQUE (member_id, book_id, active_flag) 就能在库级保证"每人每本书最多一条
--   在借记录"，同时归还历史不受限制。MySQL 允许在虚列上建唯一二级索引。
-- 金额一律 DECIMAL(12,2)，不用 FLOAT/DOUBLE。
-- fine.currency 必须是 VARCHAR(3) 而不是 CHAR(3)：FineEntity 是 String +
--   @Column(length = 3)，mysql profile 开了 ddl-auto=validate，写成 CHAR 会
--   校验失败、服务起不来（ALTER 前的建表语句里踩过一次）。
-- ============================================================================

CREATE TABLE member (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE book (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    isbn             VARCHAR(32)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(180) NOT NULL,
    category         VARCHAR(40)  NOT NULL,
    published_year   INT          NULL,
    total_copies     INT          NOT NULL DEFAULT 0,
    available_copies INT          NOT NULL DEFAULT 0,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn),
    CONSTRAINT chk_book_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE book_copy (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    book_id    BIGINT      NOT NULL,
    barcode    VARCHAR(32) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_copy_barcode (barcode),
    CONSTRAINT fk_copy_book FOREIGN KEY (book_id) REFERENCES book (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE loan (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    member_id        BIGINT      NOT NULL,
    book_id          BIGINT      NOT NULL,
    copy_id          BIGINT      NOT NULL,
    borrowed_at      DATETIME(6) NOT NULL,
    due_at           DATETIME(6) NOT NULL,
    returned_at      DATETIME(6) NULL,
    status           VARCHAR(20) NOT NULL,
    renew_count      INT         NOT NULL DEFAULT 0,
    days_overdue     INT         NOT NULL DEFAULT 0,
    active_flag      TINYINT AS (IF(status IN ('ACTIVE', 'OVERDUE'), 1, NULL)) VIRTUAL,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_loan_active_member_book (member_id, book_id, active_flag),
    CONSTRAINT fk_loan_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_loan_book FOREIGN KEY (book_id) REFERENCES book (id),
    CONSTRAINT fk_loan_copy FOREIGN KEY (copy_id) REFERENCES book_copy (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE fine (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    loan_id      BIGINT         NOT NULL,
    member_id    BIGINT         NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    currency     VARCHAR(3)    NOT NULL DEFAULT 'CNY',
    days_overdue INT            NOT NULL DEFAULT 0,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    version      BIGINT         NOT NULL DEFAULT 0,
    created_at   DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    paid_at      DATETIME(6)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fine_loan (loan_id),
    CONSTRAINT fk_fine_loan FOREIGN KEY (loan_id) REFERENCES loan (id),
    CONSTRAINT fk_fine_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT chk_fine_amount CHECK (amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
