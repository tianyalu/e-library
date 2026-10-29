package com.example.elibrary.infrastructure.messaging;

import com.example.elibrary.domain.event.DomainEvent;

/**
 * 发件箱中转用的传输抽象。
 *
 * <p>实现可以写 Kafka、写队列，也可以只打日志。关键是投递发生在业务事务<em>之后</em>，
 * 而且是至少一次投递——所以 {@link DomainEvent#eventId()} 存在，供下游去重。
 */
public interface EventBus {

    void publish(DomainEvent event);
}
