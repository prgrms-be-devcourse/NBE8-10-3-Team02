package com.back.global.igdb.dto

data class GameRow(
    val id: Long,
    val name: String?,
    val cover: IgdbCoverDto?,
    val genres: List<IgdbGenreDto>?,
)
