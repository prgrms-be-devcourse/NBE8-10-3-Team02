package com.back.global.steam

import com.back.global.steam.exception.SteamApiException
import com.back.global.steam.exception.SteamRetryableException
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.util.function.Function

@Component
class SteamRequestExecutor(
    private val steamRestClient: RestClient,
    private val props: SteamProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503)
    }

    @Retry(name = "steam")
    @RateLimiter(name = "steam")
    fun <T> execute(
        uriFunction: Function<UriBuilder, URI>,
        responseType: Class<T>,
        actionName: String,
    ): T {
        try {
            return steamRestClient
                .get()
                .uri { uriBuilder -> uriFunction.apply(uriBuilder.queryParam("key", props.apiKey)) }
                .retrieve()
                .body(responseType)!!
        } catch (e: RestClientResponseException) {
            val statusCode = e.statusCode.value()
            if (statusCode in RETRYABLE_STATUS_CODES) {
                log.warn("Steam {} 발생, 재시도 예정 - {}", statusCode, actionName)
                throw SteamRetryableException(actionName, statusCode, e)
            }
            val msg = "$actionName 실패, status: $statusCode, body: ${e.responseBodyAsString}"
            throw SteamApiException(msg, e)
        } catch (e: ResourceAccessException) {
            log.warn("Steam 네트워크 오류, 재시도 예정 - {}, cause: {}", actionName, e.message)
            throw e
        } catch (e: Exception) {
            val msg = "$actionName failed. error=${e.message}"
            throw SteamApiException(msg, e)
        }
    }
}
