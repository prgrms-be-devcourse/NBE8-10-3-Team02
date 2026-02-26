package com.back.global.igdb

import com.back.global.exception.IgdbRetryableException
import com.back.global.igdb.exception.IgdbApiException
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * 배치 전용 IGDB Request Executor.
 *
 * - batchIgdbRestClient 사용 (connect 5s / read 30s)
 *   → 500건 대용량 쿼리도 안정적으로 처리
 * - @RateLimiter(name="igdb-batch"): 배치 전용 rate limiter (timeout 5s)
 *   → 사용자 경로(200ms 즉시 포기)와 분리. 배치는 슬롯 날 때까지 5s까지 대기 허용
 * - @Retry(name="igdb-batch"): 배치 전용 재시도 설정 (4회, exponential backoff)
 */
@Component
class BatchIgdbRequestExecutor(
    private val batchIgdbRestClient: RestClient,
    private val props: IgdbProperties,
    private val tokenService: TwitchTokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503)
    }

    @Retry(name = "igdb-batch")
    @RateLimiter(name = "igdb-batch")
    fun <T> execute(
        body: String,
        responseType: Class<T>,
        endPoint: String,
        actionName: String,
    ): T {
        try {
            return batchIgdbRestClient
                .post()
                .uri(endPoint)
                .contentType(MediaType.TEXT_PLAIN)
                .header("Client-ID", props.clientId)
                .header("Authorization", "Bearer ${tokenService.getAccessToken()}")
                .body(body)
                .retrieve()
                .body(responseType)!!
        } catch (e: RestClientResponseException) {
            val statusCode = e.statusCode.value()
            if (statusCode in RETRYABLE_STATUS_CODES) {
                log.warn("IGDB(batch) {} 발생, 재시도 예정 - {}", statusCode, actionName)
                throw IgdbRetryableException(actionName, statusCode, e)
            }
            val msg = "$actionName 실패, status: $statusCode, body: ${e.responseBodyAsString}"
            throw IgdbApiException(msg, e)
        } catch (e: ResourceAccessException) {
            log.warn("IGDB(batch) 네트워크 오류, 재시도 예정 - {}, cause: {}", actionName, e.message)
            throw e
        } catch (e: Exception) {
            val msg = "$actionName failed. error=${e.message}"
            throw IgdbApiException(msg, e)
        }
    }
}
