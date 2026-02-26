package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbGameSummaryDto(
    val id: Long,
    val name: String?,
    val summary: String?,
    @JsonProperty("first_release_date")
    val firstReleaseDateEpochSeconds: Long?,
    val cover: IgdbCoverDto?,
    val genres: List<Long>?,
    val platforms: List<Long>?,
)
