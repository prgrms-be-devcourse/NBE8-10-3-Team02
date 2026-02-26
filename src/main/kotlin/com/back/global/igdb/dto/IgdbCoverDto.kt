package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbCoverDto(
    val id: Long,
    @JsonProperty("image_id")
    val imageId: String?,
)
