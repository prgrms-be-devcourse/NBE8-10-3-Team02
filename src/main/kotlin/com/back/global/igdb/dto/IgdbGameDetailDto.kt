package com.back.global.igdb.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class IgdbGameDetailDto(
    val id: Long,
    val name: String?,
    val summary: String?,
    @JsonProperty("first_release_date")
    val firstReleaseDateEpochSeconds: Long?,
    val cover: IgdbCoverDto?,
    @JsonProperty("involved_companies")
    val involvedCompanies: List<IgdbInvolvedCompanyDto>?,
    val genres: List<IgdbGenreDto>?,
    val platforms: List<IgdbPlatformDto>?,
    val storyline: String?,
    val themes: List<IgdbThemeDto>?,
    val keywords: List<IgdbKeywordDto>?,
    @JsonProperty("game_modes")
    val gameModes: List<IgdbGameModeDto>?,
    @JsonProperty("player_perspectives")
    val playerPerspectives: List<IgdbPlayerPerspectiveDto>?,
    @JsonProperty("external_games")
    val externalGames: List<IgdbExternalGameDto>?,
    val franchises: List<IgdbFranchiseDto>?,
    @JsonProperty("aggregated_rating")
    val aggregatedRating: Double?,
)
