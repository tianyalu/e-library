package com.example.elibrary.application.port.out;

import java.time.Instant;
import java.util.Optional;

/**
 * 幂等键存储。
 *
 * <p>写接口都可以带 {@code Idempotency-Key}。同一个键配同样的请求体重放，返回的是
 * <em>第一次</em>的响应而不是再执行一遍——挡的是客户端重试、手机连点两次、网络抖动。
 * 同一个键配不同的请求体就拒绝：什么都不做会把调用方的 bug 藏起来。
 */
public interface IdempotencyStorePort {

    Optional<IdempotencyRecord> find(Long memberId, String endpoint, String key);

    /**
     * 不存在才插入。
     *
     * @return true 表示这次抢到了；false 说明并发的另一次重试已经写过了
     */
    boolean trySave(IdempotencyRecord record);

    record IdempotencyRecord(Long memberId,
                             String endpoint,
                             String key,
                             String requestHash,
                             int httpStatus,
                             String responseBody,
                             Instant createdAt) {
    }
}
