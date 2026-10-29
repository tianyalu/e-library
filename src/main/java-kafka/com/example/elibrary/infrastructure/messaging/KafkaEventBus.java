package com.example.elibrary.infrastructure.messaging;

import com.example.elibrary.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka publisher used when the {@code kafka} profile is enabled ({@code docker compose --profile kafka up}).
 *
 * <p>The key is the loan id, which keeps all events of one loan in the same partition and therefore
 * ordered - the same reason trading systems partition by account or instrument.
 */
@Slf4j
@Component
@Primary
@Profile("kafka")
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaEventBus implements EventBus {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaEventBus(KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper,
                         @Value("${elibrary.messaging.topic:library.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(topic, event.aggregateId(), objectMapper.writeValueAsString(event))
                    .get(5, TimeUnit.SECONDS);
            log.debug("Published {} to {}@{}", event.eventType(), topic,
                    result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing event " + event.eventId(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish event " + event.eventId(), e);
        }
    }
}
