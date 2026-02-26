package com.back.support

import com.back.global.igdb.dto.*

object IgdbFixtures {
    fun gameDetail(igdbId: Long) =
        IgdbGameDetailDto(
            id = igdbId,
            name = "Test Game $igdbId",
            summary = "summary-$igdbId",
            firstReleaseDateEpochSeconds = 1700000000L,
            cover = IgdbCoverDto(10000L, "coverImage"),
            involvedCompanies =
                listOf(
                    IgdbInvolvedCompanyDto(10000L, IgdbCompanyDto(10000L, "testCompany"), developer = true, publisher = true),
                ),
            genres =
                listOf(
                    IgdbGenreDto(10000L, "Action"),
                    IgdbGenreDto(10001L, "RPG"),
                ),
            platforms =
                listOf(
                    IgdbPlatformDto(10001L, "PC (Windows)"),
                    IgdbPlatformDto(20001L, "PlayStation 5"),
                ),
            storyline = null,
            themes = null,
            keywords = null,
            gameModes = null,
            playerPerspectives = null,
            externalGames = null,
            franchises = null,
            aggregatedRating = null,
        )
}
