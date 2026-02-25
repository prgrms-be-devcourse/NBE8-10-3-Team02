package com.back.domain.game.game.dto

import com.back.domain.game.game.entity.Genre

data class GenreResponse(
    val id: Long, // IGDB id
    val name: String?,
) {
    companion object {
        fun from(genre: Genre): GenreResponse =
            GenreResponse(
                // 1. getIgdbId() 대신 프로퍼티 igdbId 사용
                id = genre.igdbId,
                // 2. getName() 대신 프로퍼티 name 사용
                name = genre.name,
            )
    }
}
