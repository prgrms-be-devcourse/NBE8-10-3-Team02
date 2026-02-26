package com.back.global.igdb

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

@Service
class TwitchTokenService(
    private val twitchAuthRestClient: RestClient,
    private val props: IgdbProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = AtomicReference<CachedToken>()
    private val tokenLock = ReentrantLock()

    @PostConstruct
    fun warmUpToken() {
        try {
            getAccessToken()
        } catch (e: Exception) {
            log.warn("Twitch token warm-up failed", e)
        }
    }

    fun getAccessToken(): String {
        val current = cache.get()
        if (current != null && current.expiresAt.isAfter(Instant.now().plusSeconds(30))) {
            return current.token
        }

        // 만료 30초 전이면 재발급 — 1개 스레드만 갱신, 나머지는 5초 대기
        // tryLock: Twitch 장애 시 100명이 순차적으로 무한 대기하는 문제 방지
        val acquired =
            try {
                tokenLock.tryLock(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("Twitch 토큰 갱신 대기 중 인터럽트", e)
            }
        if (!acquired) {
            throw IllegalStateException("Twitch 토큰 갱신 대기 시간 초과 (5s)")
        }
        try {
            // double-check: 대기 중 다른 스레드가 이미 갱신했을 수 있음
            val doubleCheck = cache.get()
            if (doubleCheck != null && doubleCheck.expiresAt.isAfter(Instant.now().plusSeconds(30))) {
                return doubleCheck.token
            }

            val res = fetchNewToken()
            val expiresAt = Instant.now().plusSeconds(res.expiresIn)
            cache.set(CachedToken(res.accessToken, expiresAt))
            return res.accessToken
        } finally {
            tokenLock.unlock()
        }
    }

    private fun fetchNewToken(): TokenResponse {
        val uri =
            UriComponentsBuilder
                .fromUriString(props.tokenUrl)
                .queryParam("client_id", props.clientId)
                .queryParam("client_secret", props.clientSecret)
                .queryParam("grant_type", "client_credentials")
                .build(true)
                .toUri()

        return twitchAuthRestClient
            .post()
            .uri(uri)
            .retrieve()
            .body(TokenResponse::class.java)!!
    }

    private data class CachedToken(
        val token: String,
        val expiresAt: Instant,
    )

    // Twitch OAuth 응답 주요 필드
    data class TokenResponse(
        @JsonProperty("access_token") val accessToken: String,
        @JsonProperty("expires_in") val expiresIn: Long,
        @JsonProperty("token_type") val tokenType: String?,
    )
}
