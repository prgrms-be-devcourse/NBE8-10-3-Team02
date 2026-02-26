package com.back.global.igdb.dto

data class IgdbGameBriefDto(
    val id: Long,
    val name: String?,
    val cover: IgdbCoverDto?,
)
