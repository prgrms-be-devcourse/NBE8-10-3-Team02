package com.back.global.steam.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SteamOwnedGamesResponse(
    val response: Response?,
) {
    data class Response(
        @JsonProperty("game_count") val gameCount: Int,
        val games: List<SteamGameDto>?,
    )
}
