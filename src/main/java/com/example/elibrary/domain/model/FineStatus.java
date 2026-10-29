package com.example.elibrary.domain.model;

/** 一笔逾期罚金的结清状态。 */
public enum FineStatus {
    /** 还没缴——会挡住继续借阅。 */
    PENDING,
    /** 已缴。 */
    PAID,
    /** 被管理员减免。 */
    WAIVED
}
