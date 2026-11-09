package com.logitrack.courierservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logitrack.courierservice.dto.PageMixin;
import com.logitrack.courierservice.dto.RestPageImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configuración de caché Redis para courier-service (Issue #108).
 * Define el espacio de nombres 'couriers_available' con TTL de 5 minutos,
 * serialización JSON para listas paginadas y tolerancia a caídas de Redis.
 */
@Slf4j
@Configuration("redisCacheConfig")
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    public static final String COURIERS_AVAILABLE_CACHE = "couriers_available";
    public static final Duration COURIERS_AVAILABLE_TTL = Duration.ofMinutes(5);

    private final java.util.concurrent.atomic.AtomicReference<java.time.Instant> evictionFailureExpiration =
            new java.util.concurrent.atomic.AtomicReference<>(java.time.Instant.MIN);

    public boolean isCacheReliable() {
        return java.time.Instant.now().isAfter(evictionFailureExpiration.get());
    }

    public void markEvictionFailed() {
        evictionFailureExpiration.set(java.time.Instant.now().plus(COURIERS_AVAILABLE_TTL));
    }

    public void resetDegradation() {
        evictionFailureExpiration.set(java.time.Instant.MIN);
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer courierCacheCustomizer(ObjectMapper objectMapper) {
        ObjectMapper cacheMapper = objectMapper.copy();
        cacheMapper.registerModule(new JavaTimeModule());
        cacheMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        cacheMapper.addMixIn(Page.class, PageMixin.class);
        cacheMapper.addMixIn(PageImpl.class, PageMixin.class);
        cacheMapper.addMixIn(RestPageImpl.class, PageMixin.class);
        cacheMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        var serializer = new GenericJackson2JsonRedisSerializer(cacheMapper);
        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(COURIERS_AVAILABLE_TTL)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return builder -> builder
                .withCacheConfiguration(COURIERS_AVAILABLE_CACHE, cacheConfig)
                .transactionAware();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Error al recuperar clave '{}' de la caché '{}'. Degradando a consulta directa de BD: {}",
                        key, cache != null ? cache.getName() : "desconocida", exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Error al almacenar clave '{}' en caché '{}': {}",
                        key, cache != null ? cache.getName() : "desconocida", exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Error al desalojar clave '{}' de caché '{}'. Marcando caché como no confiable para forzar lecturas desde BD: {}",
                        key, cache != null ? cache.getName() : "desconocida", exception.getMessage());
                markEvictionFailed();
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Error al limpiar caché '{}'. Marcando caché como no confiable para forzar lecturas desde BD: {}",
                        cache != null ? cache.getName() : "desconocida", exception.getMessage());
                markEvictionFailed();
            }
        };
    }
}
