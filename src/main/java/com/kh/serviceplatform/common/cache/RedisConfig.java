package com.kh.serviceplatform.common.cache;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production-ready Redis Cache configuration using Spring Cache + Lettuce.
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Value("${app.cache.default-ttl-seconds:600}")
    private long defaultTtlSeconds;

    @Value("${app.cache.service-categories-ttl-seconds:3600}")
    private long serviceCategoriesTtlSeconds;

    @Value("${app.cache.provider-profiles-ttl-seconds:900}")
    private long providerProfilesTtlSeconds;

    @Value("${app.cache.service-requests-ttl-seconds:300}")
    private long serviceRequestsTtlSeconds;

    @Value("${app.cache.provider-search-ttl-seconds:120}")
    private long providerSearchTtlSeconds;

    @Value("${app.cache.public-services-ttl-seconds:300}")
    private long publicServicesTtlSeconds;

    @Bean
    @SuppressWarnings("deprecation")
    public RedisSerializer<Object> redisJsonSerializer() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        serializer.configure(mapper -> {
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            SimpleModule pageModule = new SimpleModule("SpringDataPageModule");
            pageModule.addDeserializer(Page.class, new PageJsonDeserializer());
            pageModule.addDeserializer(PageImpl.class, new PageImplJsonDeserializer());
            mapper.registerModule(pageModule);
        });
        return serializer;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisSerializer<Object> redisJsonSerializer) {
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtlSeconds))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> initialCacheConfigs = new HashMap<>();
        initialCacheConfigs.put(CacheNames.SERVICE_CATEGORIES, defaultCacheConfig.entryTtl(Duration.ofSeconds(serviceCategoriesTtlSeconds)));
        initialCacheConfigs.put(CacheNames.PROVIDER_PROFILES, defaultCacheConfig.entryTtl(Duration.ofSeconds(providerProfilesTtlSeconds)));
        initialCacheConfigs.put(CacheNames.SERVICE_REQUESTS, defaultCacheConfig.entryTtl(Duration.ofSeconds(serviceRequestsTtlSeconds)));
        initialCacheConfigs.put(CacheNames.PROVIDER_SEARCH, defaultCacheConfig.entryTtl(Duration.ofSeconds(providerSearchTtlSeconds)));
        initialCacheConfigs.put(CacheNames.PUBLIC_SERVICES, defaultCacheConfig.entryTtl(Duration.ofSeconds(publicServicesTtlSeconds)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(initialCacheConfigs)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, RedisSerializer<Object> redisJsonSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisJsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(redisJsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    @SuppressWarnings("rawtypes")
    private static class PageJsonDeserializer extends JsonDeserializer<Page> {
        @Override
        public Page deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper codec = (ObjectMapper) p.getCodec();
            JsonNode root = codec.readTree(p);

            List<?> content = Collections.emptyList();
            JsonNode contentNode = root.get("content");
            if (contentNode != null) {
                content = codec.treeToValue(contentNode, List.class);
            }

            long total = root.has("totalElements") ? root.get("totalElements").asLong() : (content != null ? content.size() : 0);
            int pageNumber = 0;
            int pageSize = (content != null && !content.isEmpty()) ? content.size() : 20;

            JsonNode pageableNode = root.get("pageable");
            if (pageableNode != null && !pageableNode.isNull() && pageableNode.isObject()) {
                if (pageableNode.has("pageNumber")) {
                    pageNumber = pageableNode.get("pageNumber").asInt();
                }
                if (pageableNode.has("pageSize")) {
                    pageSize = pageableNode.get("pageSize").asInt();
                }
            } else if (root.has("number")) {
                pageNumber = root.get("number").asInt();
                if (root.has("size")) {
                    pageSize = root.get("size").asInt();
                }
            }

            if (pageSize <= 0) {
                pageSize = 20;
            }

            return new PageImpl<>(content != null ? content : Collections.emptyList(), PageRequest.of(pageNumber, pageSize), total);
        }
    }

    @SuppressWarnings("rawtypes")
    private static class PageImplJsonDeserializer extends JsonDeserializer<PageImpl> {
        private final PageJsonDeserializer delegate = new PageJsonDeserializer();

        @Override
        public PageImpl deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Page page = delegate.deserialize(p, ctxt);
            return (PageImpl) page;
        }
    }
}
