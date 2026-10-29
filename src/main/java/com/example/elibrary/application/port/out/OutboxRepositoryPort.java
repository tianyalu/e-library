package com.example.elibrary.application.port.out;

import com.example.elibrary.domain.event.DomainEvent;

import java.util.List;

/**
 * 事务性发件箱。
 *
 * <p>事件行由改动聚合的同一个事务插入，所以状态变化和通知不可能分家，
 * 之后由中转任务负责投递。
 */
public interface OutboxRepositoryPort {

    void append(DomainEvent event);

    List<DomainEvent> findUnpublished(int batchSize);

    void markPublished(List<String> eventIds);
}
