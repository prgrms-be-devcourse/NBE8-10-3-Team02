package com.back.global.igdb.dto

data class PopularGameCardDto(
    val id: Long,
    val name: String?,
    val coverImageId: String?,
    val genres: List<String>,
    val score: Double,
) {
    companion object {
        fun from(
            g: GameRow,
            score: Double,
        ): PopularGameCardDto {
            val genreNames = g.genres?.map { it.name } ?: emptyList()
            val coverId = g.cover?.imageId
            return PopularGameCardDto(
                id = g.id,
                name = g.name,
                coverImageId = coverId,
                genres = genreNames,
                score = score,
            )
        }
    }
}
