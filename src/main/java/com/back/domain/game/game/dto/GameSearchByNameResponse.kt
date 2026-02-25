package com.back.domain.game.game.dto

import com.back.global.igdb.dto.IgdbGameSummaryDto
import com.back.standard.util.TimeUt
import java.time.LocalDate

data class GameSearchByNameResponse(
    val igdbId: Long,
    val name: String?,
    val summary: String?,
    val firstReleaseDate: LocalDate?,
) {
    companion object {
        fun fromDto(d: IgdbGameSummaryDto): GameSearchByNameResponse =
            GameSearchByNameResponse(
                d.id,
                d.name,
                d.summary,
                TimeUt.epoch.toLocalDate(d.firstReleaseDateEpochSeconds),
            )
    }
}
