package com.back.domain.game.game.service

import com.back.domain.game.game.dto.GameDetailResponse
import com.back.domain.game.game.dto.GameVideoResponse
import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.domain.game.game.entity.CompanyRole
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.repository.GameCompanyRepository
import com.back.domain.game.game.repository.GameGenreRepository
import com.back.domain.game.game.repository.GamePlatformRepository
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.recommendation.service.GameRecommendationService
import com.back.global.exception.ServiceException
import com.back.global.igdb.IgdbDefensiveClient
import com.back.global.igdb.dto.PopularGameCardDto
import com.back.global.igdb.service.IgdbPopularRightNowService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Optional

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val gameGenreRepository: GameGenreRepository,
    private val gamePlatformRepository: GamePlatformRepository,
    private val gameCompanyRepository: GameCompanyRepository,
    private val igdbClient: IgdbDefensiveClient,
    private val igdbPopularRightNowService: IgdbPopularRightNowService,
    private val gameCacheService: GameCacheService,
    private val gameRecommendationService: GameRecommendationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * IGDB "Popular Right Now" 인기 게임 조회
     * - Visits, Want to Play, Twitch 시청 데이터 가중치 조합
     * - 캐시 사용 (30분)
     */
    fun getIgdbPopularGames(limit: Int): List<PopularGameCardDto> {
        val cacheKey = "igdb_popular_$limit"
        val cached = gameCacheService.getIgdbPopularGames(cacheKey)
        if (cached != null) return cached

        val result = igdbPopularRightNowService.popularRightNow(limit)

        gameCacheService.putIgdbPopularGames(cacheKey, result)
        return result
    }

    @Transactional(readOnly = true)
    fun getGameDetail(igdbId: Long): GameDetailResponse {
        incrementViewCountInMemory(igdbId)

        // 1. cache
        val cached = gameCacheService.getGameDetail(igdbId)
        if (cached != null) return cached

        // 2. DB
        val fromDb =
            gameRepository
                .findByIgdbId(igdbId)
                .map { assembleDetails(it) }
                .orElseThrow { ServiceException("404-1", "게임을 찾을 수 없습니다. $igdbId") }

        gameCacheService.putGameDetail(igdbId, fromDb)
        return fromDb
    }

    fun getVideoId(igdbId: Long): GameVideoResponse {
        // 1. cache에서 찾기
        val cached = gameCacheService.getVideo(igdbId)
        if (cached != null) return cached

        // 2. api호출
        val fetched = fetchVideoId(igdbId)
        gameCacheService.putVideo(igdbId, fetched)
        return fetched
    }

    fun getSimilarGames(igdbId: Long): List<SimilarGameResponse> {
        val cached = gameCacheService.getSimilarList(igdbId)
        if (cached != null) return cached

        val recommendations = gameRecommendationService.getSimilarGames(igdbId, 10)

        val result =
            recommendations.map { r ->
                SimilarGameResponse(r.gameId.toLong(), r.name, r.coverImageId)
            }

        gameCacheService.putSimilarList(igdbId, result)
        return result
    }

    private fun fetchVideoId(igdbId: Long): GameVideoResponse {
        val dto = igdbClient.getVideoId(igdbId)
        if (dto == null || dto.videoId == null) {
            return GameVideoResponse.from("")
        }
        return GameVideoResponse.from(dto.videoId)
    }

    private fun assembleDetails(game: Game): GameDetailResponse {
        val genres = gameGenreRepository.findGenreNamesByGameId(game.id!!)
        val platforms = gamePlatformRepository.findPlatformNamesByGameId(game.id!!)
        val developers = gameCompanyRepository.findCompanyNamesByGameIdAndRole(game.id!!, CompanyRole.DEVELOPER)
        val publishers = gameCompanyRepository.findCompanyNamesByGameIdAndRole(game.id!!, CompanyRole.PUBLISHER)

        return GameDetailResponse.from(game, genres, platforms, developers, publishers)
    }

    private fun incrementViewCountInMemory(igdbId: Long) {
        gameCacheService.incrementViewCount(igdbId)
    }

    // 5분마다 캐시에 있는 조회수를 DB에 반영
    @Scheduled(fixedRate = 300000) // 5분
    @Transactional
    fun flushViewCountsToDb() {
        val snapshot = gameCacheService.getViewCountSnapshot()

        if (snapshot.isEmpty()) return

        log.info("조회수 DB 반영 시작: {} 건", snapshot.size)

        snapshot.forEach { (igdbId, counter) ->
            val delta = counter.getAndSet(0) // 가져오고 0으로 리셋
            if (delta > 0) {
                try {
                    gameRepository.incrementViewCount(igdbId, delta)
                } catch (e: Exception) {
                    log.warn("조회수 반영 실패: igdbId={}, delta={}", igdbId, delta, e)
                    // 실패한 건 다시 더해줌
                    counter.addAndGet(delta)
                }
            }
        }

        log.info("조회수 DB 반영 완료")
    }

    fun findById(id: Long): Optional<Game> = gameRepository.findById(id)

    fun createGame(
        igdbId: Long,
        name: String?,
        summary: String,
        coverImage: String?,
        firstReleaseDate: LocalDate?,
    ): Game {
        val game = Game.createGame(igdbId, name, summary, coverImage, firstReleaseDate)
        return gameRepository.save(game)
    }
}
