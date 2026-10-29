-- ============================================================================
-- V2: 运维表与配套索引
--
-- outbox_event：事务性发件箱，只追加。业务数据变更和事件行写在同一个事务里，
--   所以事件既不会丢，也不会凭空多出来。
-- idempotency_record：写接口的幂等键。靠 UNIQUE (member_id, endpoint, key) 把
--   重复重试变成"重放首次响应"或冲突响应，不会产生第二笔业务副作用。
-- ============================================================================

CREATE TABLE outbox_event (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    event_id       VARCHAR(36) NOT NULL,
    event_type     VARCHAR(60) NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id   VARCHAR(64) NOT NULL,
    payload        JSON        NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE idempotency_record (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    member_id        BIGINT      NOT NULL,
    endpoint         VARCHAR(80) NOT NULL,
    idempotency_key  VARCHAR(80) NOT NULL,
    request_hash     VARCHAR(64) NOT NULL,
    http_status      INT         NOT NULL DEFAULT 200,
    response_body    JSON        NOT NULL,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_idem_member_endpoint_key (member_id, endpoint, idempotency_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- read/search
CREATE INDEX idx_book_title ON book (title);
CREATE INDEX idx_book_author ON book (author);
CREATE INDEX idx_book_category ON book (category);

-- hot paths
CREATE INDEX idx_copy_book_status ON book_copy (book_id, status);
CREATE INDEX idx_loan_member_status ON loan (member_id, status);
CREATE INDEX idx_loan_due_at ON loan (due_at);
CREATE INDEX idx_loan_status ON loan (status);
CREATE INDEX idx_fine_member_status ON fine (member_id, status);

-- outbox relay scans unpublished events in creation order
CREATE INDEX idx_outbox_unpublished ON outbox_event (published_at, id);
