package com.back.global.config

import com.back.domain.game.game.dto.GameDetailResponse
import com.back.domain.game.game.dto.GameVideoResponse
import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.global.igdb.dto.PopularGameCardDto
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

@Configuration
@EnableCaching // - @Cacheable 어노테이션 사용을 위해 필수!
class CacheConfig {

    /**
     * [1] 보안 및 멤버 서비스 전용 CacheManager (자동 캐싱용)
     * findByEmail, findByApiKey, Bcrypt 연산 결과를 관리합니다.
     */
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()

        val caches = listOf(
            createCaffeineCache("apiKeys", Duration.ofMinutes(30), 10000),

            createCaffeineCache("passwordMatches", Duration.ofMinutes(5), 1000)
        )

        cacheManager.setCaches(caches)
        return cacheManager
    }

    private fun createCaffeineCache(name: String, ttl: Duration, size: Long): CaffeineCache {
        return CaffeineCache(name, Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(size)
            .build())
    }

    @Bean
    fun gameDetailCache(): Cache<Long, GameDetailResponse> =
        Caffeine.newBuilder().maximumSize(20000).expireAfterWrite(Duration.ofHours(12)).build()

    @Bean
    fun videoIdCache(): Cache<Long, GameVideoResponse> =
        Caffeine.newBuilder().maximumSize(20000).expireAfterWrite(Duration.ofDays(10)).build()

    @Bean
    fun similarIdsCache(): Cache<Long, List<Long>> =
        Caffeine.newBuilder().maximumSize(20000).expireAfterWrite(Duration.ofHours(24)).build()

    @Bean
    fun similarListCache(): Cache<Long, List<SimilarGameResponse>> =
        Caffeine.newBuilder().maximumSize(20000).expireAfterWrite(Duration.ofHours(24)).build()

    @Bean
    fun viewCountCache(): Cache<Long, AtomicLong> =
        Caffeine.newBuilder().maximumSize(10000).build()

    @Bean
    fun igdbPopularGamesCache(): Cache<String, List<PopularGameCardDto>> =
        Caffeine.newBuilder().maximumSize(10).expireAfterWrite(Duration.ofMinutes(30)).build()
}