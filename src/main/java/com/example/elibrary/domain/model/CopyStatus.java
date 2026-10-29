package com.example.elibrary.domain.model;

/** 一册实物的状态。 */
public enum CopyStatus {
    /** 在架上，可借。 */
    AVAILABLE,
    /** 已借出。 */
    ON_LOAN,
    /** 暂时不能提供服务（损坏、补货中）。 */
    MAINTENANCE
}
