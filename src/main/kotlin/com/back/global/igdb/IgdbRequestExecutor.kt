package com.back.global.igdb

import com.back.global.exception.IgdbRetryableException
import com.back.global.igdb.exception.IgdbApiException
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class IgdbRequestExecutor(
    private val apiIgdbRestClient: RestClient,
    private val props: IgdbProperties,
    private val tokenService: TwitchTokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503)
    }

    fun <T> execute(
        body: String,
        responseType: Class<T>,
        endPoint: String,
        actionName: String,
    ): T {
        try {
            return apiIgdbRestClient
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
                log.warn("IGDB {} 발생, 재시도 예정 - {}", statusCode, actionName)
                throw IgdbRetryableException(actionName, statusCode, e)
            }
            val msg = "$actionName 실패, status: $statusCode, body: ${e.responseBodyAsString}"
            throw IgdbApiException(msg, e)
        } catch (e: ResourceAccessException) {
            log.warn("IGDB 네트워크 오류, 재시도 예정 - {}, cause: {}", actionName, e.message)
            throw e
        } catch (e: Exception) {
            val msg = "$actionName failed. error=${e.message}"
            throw IgdbApiException(msg, e)
        }
    }
}
