package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbPopularityPrimitiveDto(
    val id: Long,
    @JsonProperty("game_id")
    val gameId: Long?,
    val value: Double?,
    @JsonProperty("popularity_type")
    val popularityType: Int?,
)
