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

// 1. 생성자 주입: 클래스 선언부에 바로 생성자를 작성합니다. (롬복 필요 없음)
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
    ): GameDetailResponse? {
        // 2. !! 제거: 이제 gameService는 절대 null이 아니므로 바로 호출 가능합니다.
        return gameService.getGameDetail(igdbId)
    }

    @GetMapping("/games/{igdbId}/video")
    @Operation(summary = "게임 영상 조회", description = "게임 상세 정보에 사용될 videoId조회")
    fun getVideoId(
        @PathVariable igdbId: Long,
    ): GameVideoResponse? = gameService.getVideoId(igdbId)

    @GetMapping("/games/{igdbId}/similarGames")
    @Operation(summary = "비슷한 게임 조회", description = "게임 상세 정보에 사용될 비슷한 게임 목록 조회")
    fun getSimilarGames(
        @PathVariable igdbId: Long,
    ): List<SimilarGameResponse?> {
        // 3. List 타입 단순화: MutableList 대신 일반 List를 사용하는 것이 안전합니다.
        return gameService.getSimilarGames(igdbId) ?: emptyList()
    }

    @GetMapping("/games/popular/igdb")
    @Operation(summary = "IGDB 인기 게임 조회", description = "IGDB Popular Right Now")
    fun getIgdbPopularGames(
        @RequestParam(defaultValue = "10") limit: Int,
    ): List<PopularGameCardDto?> = gameService.getIgdbPopularGames(limit) ?: emptyList()

    @GetMapping("/games/search")
    fun search(
        @RequestParam query: String?,
        @RequestParam(required = false) genre: List<Long>?,
        @RequestParam(required = false) platform: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): List<GameSearchResponse?> {
        // 4. 프로퍼티 직접 할당으로 수정 (빨간 줄 해결!)
        val condition =
            GameSearchCondition().apply {
                this.query = query // setQuery(query) 대신
                this.genreIds = genre?.toMutableList() // setGenreIds(genre) 대신
                this.platformCode = platform // setPlatformCode(platform) 대신
                this.page = page // setPage(page) 대신
                this.size = size // setSize(size) 대신
            }

        return gameSearchService.search(condition) ?: emptyList()
    }

    // 5. 프로퍼티 활용: getXxx() 대신 프로퍼티 문법 사용
    @GetMapping("/genres")
    fun getGenres(): List<GenreResponse?> = genreService.getGenres() ?: emptyList()

    @GetMapping("/platforms")
    fun getPlatforms(): List<PlatformResponse> {
        // 6. 코틀린스러운 컬렉션 처리 (stream 대신 map 활용)
        return PlatformGroup.DISPLAY_NAME.map { (key, value) ->
            PlatformResponse(key, value)
        }
    }
}
