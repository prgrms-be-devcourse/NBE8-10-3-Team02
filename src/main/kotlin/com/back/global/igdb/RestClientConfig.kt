package com.back.global.igdb

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

/**
 * IGDB용 RestClient 두 종류:
 *
 * apiIgdbRestClient  — 사용자 요청 경로 (IgdbRequestExecutor)
 *   connect 1s / read 1.5s : 총 예산 ~2.5s → 사용자 대기 최대 2.5s 내 DB fallback
 *
 * batchIgdbRestClient — 배치 경로 (BatchIgdbRequestExecutor)
 *   connect 5s / read 30s : 500건 대용량 쿼리 허용
 *
 * 두 Executor가 동일한 @RateLimiter(name="igdb")를 공유하므로
 * 배치 + API 합산 4 req/s 로 IGDB rate limit 준수.
 * RateLimiter timeout 200ms: 슬롯 즉시 없으면 RequestNotPermitted → 즉시 fallback (대기 없음)
 */
@Configuration
class RestClientConfig {
    companion object {
        // ── API 경로 (사용자 요청 전용) ──────────────────────────────────
        private val API_CONNECT_TIMEOUT = Duration.ofSeconds(1)
        private val API_READ_TIMEOUT = Duration.ofMillis(1500)

        // ── 배치 경로 (대용량 쿼리 전용) ─────────────────────────────────
        private val BATCH_CONNECT_TIMEOUT = Duration.ofSeconds(5)
        private val BATCH_READ_TIMEOUT = Duration.ofSeconds(30)
    }

    @Bean
    fun apiIgdbRestClient(
        builder: RestClient.Builder,
        props: IgdbProperties,
    ): RestClient {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(API_CONNECT_TIMEOUT)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(API_READ_TIMEOUT)
        return builder
            .baseUrl(props.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun batchIgdbRestClient(
        builder: RestClient.Builder,
        props: IgdbProperties,
    ): RestClient {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(BATCH_CONNECT_TIMEOUT)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(BATCH_READ_TIMEOUT)
        return builder
            .baseUrl(props.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun twitchAuthRestClient(builder: RestClient.Builder): RestClient {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(Duration.ofSeconds(5))
        // tokenUrl은 full url로 요청할 거라 baseUrl 없음
        return builder
            .requestFactory(requestFactory)
            .build()
    }
}
