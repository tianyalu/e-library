package com.example.elibrary.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * 目录读的 Redis 缓存。三个决定：
 * <ul>
 *   <li><b>带类型信息的 JSON</b> 而不是 JDK 序列化：缓存里的内容人能看懂，
 *       给 DTO 加个字段也不会让老条目反序列化失败；</li>
 *   <li><b>短 TTL + 写后主动失效</b>：可借数随时会变，正确性主要靠过期时间和失效；</li>
 *   <li><b>库存不进缓存</b>：能不能借由数据库事务说了算，缓存只负责让读变快。</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheConfig {

    public static final String CATALOGUE_CACHE = "catalogue";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        RedisSerializationContext.SerializationPair<Object> pair =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(mapper));

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(pair)
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                CATALOGUE_CACHE, defaults.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
