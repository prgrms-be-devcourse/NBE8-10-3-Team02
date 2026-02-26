package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbPopularGameDto(
    val id: Long,
    val name: String?,
    val cover: IgdbCoverDto?,
    @JsonProperty("total_rating")
    val totalRating: Double?,
    @JsonProperty("total_rating_count")
    val totalRatingCount: Int?,
)
