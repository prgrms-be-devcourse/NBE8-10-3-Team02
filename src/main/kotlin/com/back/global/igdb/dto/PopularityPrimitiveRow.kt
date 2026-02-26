package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class PopularityPrimitiveRow(
    val id: Long,
    @JsonProperty("game_id") val gameId: Long,
    val value: BigDecimal?,
    @JsonProperty("popularity_type") val popularityType: Int,
)
