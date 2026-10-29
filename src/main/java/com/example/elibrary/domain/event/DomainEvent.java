package com.example.elibrary.domain.event;

import java.time.Instant;

/**
 * 已经发生、可以往下游投递的事。
 *
 * <p>事件在同一个数据库事务里追加（事务性发件箱），
 * 所以事件既不会丢，也不会凭空造出来。
 */
public interface DomainEvent {

    String eventId();

    String eventType();

    String aggregateType();

    String aggregateId();

    Instant occurredAt();
}
