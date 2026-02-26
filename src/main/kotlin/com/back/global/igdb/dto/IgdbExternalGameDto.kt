package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbExternalGameDto(
    val id: Long,
    @JsonProperty("external_game_source")
    val externalGameSource: Long?,
    val uid: String?,
)
