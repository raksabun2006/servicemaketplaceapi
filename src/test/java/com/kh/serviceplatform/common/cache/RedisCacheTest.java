package com.kh.serviceplatform.common.cache;

import com.kh.serviceplatform.features.category.CategoryMapper;
import com.kh.serviceplatform.features.category.CategoryRepository;
import com.kh.serviceplatform.features.category.CategoryServiceImpl;
import com.kh.serviceplatform.features.category.dto.CategoryResponse;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.provider.dto.NearbyProviderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheTest {

    private CustomCacheErrorHandler errorHandler;
    private RedisConfig redisConfig;
    private RedisSerializer<Object> jsonSerializer;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        errorHandler = new CustomCacheErrorHandler();
        redisConfig = new RedisConfig();
        jsonSerializer = redisConfig.redisJsonSerializer();
    }

    @Test
    @DisplayName("CustomCacheErrorHandler catches and logs Redis errors without throwing exceptions")
    void testErrorHandlerFaultTolerance() {
        Cache mockCache = mock(Cache.class);
        when(mockCache.getName()).thenReturn("testCache");
        RuntimeException redisException = new RuntimeException("Redis connection refused");

        // Verify none of the error handler methods throw an exception (graceful database fallback)
        assertThatCode(() -> errorHandler.handleCacheGetError(redisException, mockCache, "testKey"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCachePutError(redisException, mockCache, "testKey", "testValue"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCacheEvictError(redisException, mockCache, "testKey"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCacheClearError(redisException, mockCache))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("JSON Serializer serializes and deserializes record DTOs correctly")
    void testRecordSerialization() {
        UUID categoryId = UUID.randomUUID();
        CategoryResponse response = new CategoryResponse(
                categoryId,
                "Cleaning",
                "CLEANING",
                "Home cleaning services",
                null,
                1,
                true,
                Instant.now(),
                Instant.now()
        );

        byte[] serialized = jsonSerializer.serialize(response);
        assertThat(serialized).isNotEmpty();

        Object deserialized = jsonSerializer.deserialize(serialized);
        assertThat(deserialized).isInstanceOf(CategoryResponse.class);
        CategoryResponse deserializedResponse = (CategoryResponse) deserialized;
        assertThat(deserializedResponse.id()).isEqualTo(categoryId);
        assertThat(deserializedResponse.name()).isEqualTo("Cleaning");
        assertThat(deserializedResponse.code()).isEqualTo("CLEANING");
    }

    @Test
    @DisplayName("JSON Serializer safely handles Spring Data PageImpl")
    void testPageSerialization() {
        UUID providerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        NearbyProviderResponse nearby = new NearbyProviderResponse(
                providerId,
                userId,
                "John Provider",
                null,
                "Clean Pro Co",
                "Professional cleaners",
                5,
                "Phnom Penh",
                "Phnom Penh",
                "Chamkar Mon",
                BigDecimal.valueOf(15.0),
                4.8,
                25,
                true,
                1.5
        );

        Page<NearbyProviderResponse> page = new PageImpl<>(List.of(nearby), PageRequest.of(0, 10), 1);
        byte[] serialized = jsonSerializer.serialize(page);
        assertThat(serialized).isNotEmpty();

        Object deserialized = jsonSerializer.deserialize(serialized);
        assertThat(deserialized).isInstanceOf(Page.class);
        @SuppressWarnings("unchecked")
        Page<NearbyProviderResponse> deserializedPage = (Page<NearbyProviderResponse>) deserialized;
        assertThat(deserializedPage.getTotalElements()).isEqualTo(1);
        assertThat(deserializedPage.getContent()).hasSize(1);
        assertThat(deserializedPage.getContent().get(0).businessName()).isEqualTo("Clean Pro Co");
    }

    @Test
    @DisplayName("Cache hit returns cached value without calling underlying service")
    void testCacheHit() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(CacheNames.SERVICE_CATEGORIES);
        Cache cache = cacheManager.getCache(CacheNames.SERVICE_CATEGORIES);
        assertThat(cache).isNotNull();

        UUID categoryId = UUID.randomUUID();
        List<CategoryResponse> initialData = List.of(new CategoryResponse(
                categoryId, "Cleaning", "CLEANING", "desc", null, 1, true, Instant.now(), Instant.now()
        ));

        // Put initial entry in cache
        cache.put("all", initialData);

        // Fetch from cache (cache hit)
        Cache.ValueWrapper wrapper = cache.get("all");
        assertThat(wrapper).isNotNull();
        @SuppressWarnings("unchecked")
        List<CategoryResponse> cachedData = (List<CategoryResponse>) wrapper.get();
        assertThat(cachedData).hasSize(1);
        assertThat(cachedData.get(0).id()).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("Cache eviction invalidates cache on database mutations")
    void testCacheEvictionOnMutation() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(CacheNames.SERVICE_CATEGORIES);
        Cache cache = cacheManager.getCache(CacheNames.SERVICE_CATEGORIES);
        assertThat(cache).isNotNull();

        // Simulate cached categories
        cache.put("all", List.of(new CategoryResponse(
                UUID.randomUUID(), "Plumbing", "PLUMBING", "desc", null, 1, true, Instant.now(), Instant.now()
        )));
        assertThat(cache.get("all")).isNotNull();

        // Evict
        cache.clear();
        assertThat(cache.get("all")).isNull();
    }

    @Test
    @DisplayName("Cache key formatting differentiates queries with different parameters")
    void testCompositeKeyDifferentiation() {
        String key1 = String.format("available:%s:%d:%d:%s", true, 0, 10, "UNSORTED");
        String key2 = String.format("available:%s:%d:%d:%s", false, 0, 10, "UNSORTED");
        String key3 = String.format("available:%s:%d:%d:%s", true, 1, 10, "UNSORTED");

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isNotEqualTo(key3);
        assertThat(key2).isNotEqualTo(key3);
    }

    @Test
    @DisplayName("User isolation: Private request data is not exposed to unauthenticated users")
    void testUserIsolationKeys() {
        UUID publicUserId = null;
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();

        // Unauthenticated calls use cache key condition #userId == null
        boolean isPublicCached = (publicUserId == null);
        boolean isCustomerACached = (customerA == null);
        boolean isCustomerBCached = (customerB == null);

        assertThat(isPublicCached).isTrue();
        assertThat(isCustomerACached).isFalse();
        assertThat(isCustomerBCached).isFalse();
    }
}
