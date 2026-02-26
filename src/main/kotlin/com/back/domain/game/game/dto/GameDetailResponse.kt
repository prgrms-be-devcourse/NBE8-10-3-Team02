package com.back.domain.game.game.dto

import com.back.domain.game.game.entity.Game
import java.time.LocalDate

data class GameDetailResponse(
    val gameId: Long,
    val igdbId: Long,
    val gameName: String?,
    val summary: String,
    val firstReleaseDate: LocalDate?,
    val coverImageId: String?,
    val coverUrlTemplate: String?,
    val developers: List<String>,
    val publishers: List<String>,
    val genres: List<String>,
    val platforms: List<String>,
) {
    companion object {
        private const val COVER_URL_TEMPLATE =
            "https://images.igdb.com/igdb/image/upload/{size}/{id}.jpg"

        @JvmStatic
        fun from(
            game: Game,
            genres: List<String>,
            platforms: List<String>,
            developers: List<String>,
            publishers: List<String>,
        ): GameDetailResponse {
            val coverImageId = game.coverImageId
            return GameDetailResponse(
                gameId = game.id!!,
                igdbId = game.igdbId,
                gameName = game.name,
                summary = game.summary,
                firstReleaseDate = game.firstReleaseDate,
                coverImageId = coverImageId,
                coverUrlTemplate = if (coverImageId != null) COVER_URL_TEMPLATE else null,
                developers = developers,
                publishers = publishers,
                genres = genres,
                platforms = platforms,
            )
        }
    }
}
