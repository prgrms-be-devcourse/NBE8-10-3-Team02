package com.back.domain.game.game.controller

import com.back.domain.game.game.dto.GameDetailResponse
import com.back.domain.game.game.dto.GameSearchCondition
import com.back.domain.game.game.dto.GameSearchResponse
import com.back.domain.game.game.dto.GameVideoResponse
import com.back.domain.game.game.dto.GenreResponse
import com.back.domain.game.game.dto.PlatformResponse
import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.domain.game.game.service.GameSearchService
import com.back.domain.game.game.service.GameService
import com.back.domain.game.game.service.GenreService
import com.back.domain.game.platform.PlatformGroup
import com.back.global.igdb.dto.PopularGameCardDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Tag(name = "ApiV1GameController", description = "게임 검색과 상세조회 API")
class ApiV1GameController(
    private val gameService: GameService,
    private val gameSearchService: GameSearchService,
    private val genreService: GenreService,
) {
    @GetMapping("/games/{igdbId}")
    @Operation(summary = "게임 상세 조회", description = "IGDB 게임 상세 조회")
    fun getGameDetail(
        @PathVariable igdbId: Long,
    ): GameDetailResponse = gameService.getGameDetail(igdbId)

    @GetMapping("/games/{igdbId}/video")
    @Operation(summary = "게임 영상 조회", description = "게임 상세 정보에 사용될 videoId조회")
    fun getVideoId(
        @PathVariable igdbId: Long,
    ): GameVideoResponse = gameService.getVideoId(igdbId)

    @GetMapping("/games/{igdbId}/similarGames")
    @Operation(summary = "비슷한 게임 조회", description = "게임 상세 정보에 사용될 비슷한 게임 목록 조회")
    fun getSimilarGames(
        @PathVariable igdbId: Long,
    ): List<SimilarGameResponse> = gameService.getSimilarGames(igdbId)

    @GetMapping("/games/popular/igdb")
    @Operation(
        summary = "IGDB 인기 게임 조회",
        description = "IGDB Popular Right Now (Visits + Want + Twitch 가중치 조합)",
    )
    fun getIgdbPopularGames(
        @RequestParam(defaultValue = "10") limit: Int,
    ): List<PopularGameCardDto> = gameService.getIgdbPopularGames(limit)

    //    슬기구현
    @GetMapping("/games/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(required = false) genre: List<Long>?,
        @RequestParam(required = false) platform: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): List<GameSearchResponse> {
        val condition =
            GameSearchCondition(
                query = query,
                genreIds = genre,
                platformCode = platform,
                page = page,
                size = size,
            )
        return gameSearchService.search(condition)
    }

    //    장르 필터링
    @GetMapping("/genres")
    fun getGenres(): List<GenreResponse> = genreService.getGenres()

    //    플랫폼 필터링
    @GetMapping("/platforms")
    fun getPlatforms(): List<PlatformResponse> = PlatformGroup.DISPLAY_NAME.map { (code, name) -> PlatformResponse(code, name) }
}
