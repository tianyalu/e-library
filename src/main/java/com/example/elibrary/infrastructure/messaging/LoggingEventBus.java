package com.example.elibrary.infrastructure.messaging;

import com.example.elibrary.domain.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认实现：不需要消息中间件，服务在哪都能跑。
 *
 * <p>条件用<em>字符串</em>写类名是有原因的：写成 class 字面量的话，
 * 默认运行时（classpath 里没有 {@code spring-kafka}）在解析注解值时就会炸。
 * 这里的失败会让中转循环直接停住——broker 不通的时候还把数据标成"已发布"，
 * 正是那种看不见的丢失，绝不能接受。
 */
@Slf4j
@Component
@ConditionalOnMissingBean(type = "com.example.elibrary.infrastructure.messaging.KafkaEventBus")
public class LoggingEventBus implements EventBus {

    @Override
    public void publish(DomainEvent event) {
        log.info("EVENT {} [{}] aggregate={}#{}", event.eventType(), event.eventId(),
                event.aggregateType(), event.aggregateId());
    }
}
