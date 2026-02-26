package com.back.global.steam

import com.back.global.steam.dto.SteamGameDto
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SteamCircuitBreakerClient(
    private val steamClient: SteamClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "steam", fallbackMethod = "getOwnedGamesFallback")
    @Bulkhead(name = "steam", fallbackMethod = "getOwnedGamesFallback")
    fun getOwnedGames(steamId: String): List<SteamGameDto> = steamClient.getOwnedGames(steamId)

    private fun getOwnedGamesFallback(
        steamId: String,
        t: Throwable,
    ): List<SteamGameDto> {
        log.warn("Steam fallback – getOwnedGames(steamId={}), cause: {}", steamId, t.message)
        return emptyList()
    }
}
