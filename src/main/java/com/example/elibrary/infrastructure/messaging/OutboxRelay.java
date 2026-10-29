package com.example.elibrary.infrastructure.messaging;

import com.example.elibrary.application.port.out.OutboxRepositoryPort;
import com.example.elibrary.config.LibraryProperties;
import com.example.elibrary.domain.event.DomainEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 把库里的事件搬到 broker。
 *
 * <p>自己跑在一个短事务里：取一批 → 投递 → 打上 {@code published_at}。
 * 中途任何一步失败，一个都不标记，整批重试。投递本就是至少一次，
 * 所以下游要按 {@code eventId} 去重。
 */
@Slf4j
@Component
public class OutboxRelay {

    private final OutboxRepositoryPort outbox;
    private final EventBus eventBus;
    private final int batchSize;
    private final MeterRegistry meterRegistry;

    public OutboxRelay(OutboxRepositoryPort outbox, EventBus eventBus, LibraryProperties properties,
                       MeterRegistry meterRegistry) {
        this.outbox = outbox;
        this.eventBus = eventBus;
        this.batchSize = properties.outbox().batchSize();
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${elibrary.outbox.delay-millis:1500}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void relay() {
        List<DomainEvent> pending = outbox.findUnpublished(batchSize);
        if (pending.isEmpty()) {
            return;
        }
        for (DomainEvent event : pending) {
            eventBus.publish(event);
        }
        outbox.markPublished(pending.stream().map(DomainEvent::eventId).toList());
        meterRegistry.counter("elibrary.outbox.published").increment(pending.size());
        log.debug("Relayed {} event(s)", pending.size());
    }
}
