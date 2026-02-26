package com.back.domain.game.game.dto

import com.back.domain.game.game.entity.Genre

data class GenreResponse(
    val id: Long, // IGDB id
    val name: String,
) {
    companion object {
        fun from(genre: Genre) =
            GenreResponse(
                id = genre.igdbId,
                name = genre.name,
            )
    }
}
