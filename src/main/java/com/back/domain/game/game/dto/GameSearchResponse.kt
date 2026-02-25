package com.back.domain.game.game.dto

import com.back.global.igdb.dto.IgdbGameSummaryDto
import com.back.global.igdb.util.IgdbImageUtil
import com.back.standard.util.TimeUt
import java.time.LocalDate

data class GameSearchResponse(
    val igdbId: Long,
    val name: String?,
    val imageUrl: String?,
    val firstReleaseDate: LocalDate?,
    val genres: List<String?>?,
    val platforms: List<String?>?,
) {
    companion object {
        fun fromDto(
            d: IgdbGameSummaryDto,
            genreMap: Map<Long?, String?>,
            platformMap: Map<Long?, String?>,
        ): GameSearchResponse =
            GameSearchResponse(
                igdbId = d.id,
                name = d.name,
                imageUrl = IgdbImageUtil.cover(d.cover?.imageId),
                firstReleaseDate = TimeUt.epoch.toLocalDate(d.firstReleaseDateEpochSeconds),
                genres = d.genres?.mapNotNull { genreMap[it] } ?: emptyList(),
                platforms = d.platforms?.mapNotNull { platformMap[it] } ?: emptyList(),
            )
    }
}
