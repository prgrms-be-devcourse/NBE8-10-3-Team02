package com.back.global.steam

import com.back.global.steam.dto.SteamGameDto
import com.back.global.steam.dto.SteamOwnedGamesResponse
import org.springframework.stereotype.Component

@Component
class SteamClient(
    private val requestExecutor: SteamRequestExecutor,
) {
    // https://partner.steamgames.com/doc/webapi/IPlayerService#GetOwnedGames
    companion object {
        private const val OWNED_GAMES_ENDPOINT = "/IPlayerService/GetOwnedGames/v1/"
    }

    fun getOwnedGames(steamId: String): List<SteamGameDto> {
        val response =
            requestExecutor.execute(
                { uriBuilder ->
                    uriBuilder
                        .path(OWNED_GAMES_ENDPOINT)
                        .queryParam("steamid", steamId)
                        .queryParam("include_appinfo", true)
                        .queryParam("include_played_free_games", true)
                        .queryParam("format", "json")
                        .build()
                },
                SteamOwnedGamesResponse::class.java,
                "getOwnedGames(steamId=$steamId)",
            )

        return response.response?.games ?: emptyList()
    }
}
