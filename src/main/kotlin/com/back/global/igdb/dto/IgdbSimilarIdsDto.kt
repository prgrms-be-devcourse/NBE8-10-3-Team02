package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbSimilarIdsDto(
    val id: Long,
    @JsonProperty("similar_games")
    val similarGames: List<Long>?,
)
