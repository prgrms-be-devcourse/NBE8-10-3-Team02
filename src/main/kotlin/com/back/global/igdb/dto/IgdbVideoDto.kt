package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbVideoDto(
    val id: Long,
    @JsonProperty("video_id")
    val videoId: String?,
)
