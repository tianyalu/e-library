package com.example.elibrary.application.service;

import com.example.elibrary.application.port.out.IdempotencyStorePort;
import com.example.elibrary.domain.exception.ConflictException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * 写接口的"最多执行一次"：幂等键 + 库里的唯一约束（见 {@code idempotency_record}）。
 *
 * <p>真正兜底的是那条唯一索引，不是"先查再插"。并发写撞车只说明"别人已经做过了"，
 * 那就重放赢家当时的响应，而不是再跑一遍业务逻辑。
 */
@Slf4j
@Service
public class IdempotencyService {

    private final IdempotencyStorePort store;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyStorePort store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    /** 包一层：告诉调用方这个响应体是本次新做的，还是从库里重放的。 */
    public record Outcome<T>(T body, boolean replayed) {

        public static <T> Outcome<T> fresh(T body) {
            return new Outcome<>(body, false);
        }

        public static <T> Outcome<T> replay(T body) {
            return new Outcome<>(body, true);
        }
    }

    /**
     * @param key          调用方传的幂等键，不传或空字符串表示"我不关心重试"，那就直接放行
     * @param request      请求体，用来算哈希，识别"同一个键配了不同数据"
     * @param responseType 重放时把存储的字符串反序列化成什么类型
     * @param action       真正的业务逻辑，每个键最多执行一次
     */
    public <T> Outcome<T> execute(Long memberId,
                                  String endpoint,
                                  String key,
                                  Object request,
                                  Class<T> responseType,
                                  Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return Outcome.fresh(action.get());
        }
        String requestHash = sha256(request);
        var existing = store.find(memberId, endpoint, key);
        if (existing.isPresent()) {
            requireSameHash(existing.get(), requestHash, key);
            log.debug("Idempotency hit for key={} endpoint={} memberId={}", key, endpoint, memberId);
            return Outcome.replay(deserialize(existing.get(), responseType));
        }

        T body = action.get();
        boolean stored = store.trySave(new IdempotencyStorePort.IdempotencyRecord(
                memberId, endpoint, key, requestHash, 201, serialize(body), Instant.now()));
        if (!stored) {
            var winner = store.find(memberId, endpoint, key)
                    .orElseThrow(() -> ConflictException.idempotencyKeyReused(key));
            requireSameHash(winner, requestHash, key);
            log.info("Lost idempotency race for key={} endpoint={}; replaying winner response", key, endpoint);
            return Outcome.replay(deserialize(winner, responseType));
        }
        return Outcome.fresh(body);
    }

    private void requireSameHash(IdempotencyStorePort.IdempotencyRecord record, String requestHash, String key) {
        if (!record.requestHash().equals(requestHash)) {
            throw ConflictException.idempotencyKeyReused(key);
        }
    }

    private <T> T deserialize(IdempotencyStorePort.IdempotencyRecord record, Class<T> type) {
        try {
            return objectMapper.readValue(record.responseBody(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored idempotent response cannot be deserialized", e);
        }
    }

    private String serialize(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Response cannot be serialized for idempotency storage", e);
        }
    }

    private String sha256(Object request) {
        try {
            byte[] payload = request == null ? new byte[0] : objectMapper.writeValueAsBytes(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException("Cannot hash request payload", e);
        }
    }
}
