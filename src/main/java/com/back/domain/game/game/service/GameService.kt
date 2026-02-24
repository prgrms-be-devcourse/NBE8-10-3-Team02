package com.back.domain.game.game.service

import com.back.domain.game.game.dto.*
import com.back.domain.game.game.entity.CompanyRole
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.repository.*
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
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@Service // 1. @RequiredArgsConstructor, @Slf4j 삭제
class GameService(
    // 2. 생성자 주입 방식으로 변경 (null 허용 안 함)
    private val gameRepository: GameRepository,
    private val gameGenreRepository: GameGenreRepository,
    private val gamePlatformRepository: GamePlatformRepository,
    private val gameCompanyRepository: GameCompanyRepository,
    private val igdbClient: IgdbDefensiveClient,
    private val igdbPopularRightNowService: IgdbPopularRightNowService,
    private val gameCacheService: GameCacheService,
    private val gameRecommendationService: GameRecommendationService
) {
    // 3. 로그 객체 직접 선언
    private val log = LoggerFactory.getLogger(javaClass)

    fun getIgdbPopularGames(limit: Int): List<PopularGameCardDto> {
        val cacheKey = "igdb_popular_$limit"
        // 4. !! 대신 안전한 호출(?.)과 엘비스 연산자(?:) 사용
        return gameCacheService.getIgdbPopularGames(cacheKey) ?: run {
            val result = igdbPopularRightNowService.popularRightNow(limit)
            gameCacheService.putIgdbPopularGames(cacheKey, result)
            result
        }
    }

    @Transactional(readOnly = true)
    fun getGameDetail(igdbId: Long): GameDetailResponse {
        incrementViewCountInMemory(igdbId)

        return gameCacheService.getGameDetail(igdbId) ?: run {
            val fromDb = gameRepository.findByIgdbId(igdbId)
                .map { assembleDetails(it) }
                .orElseThrow { ServiceException("404-1", "게임을 찾을 수 없습니다. $igdbId") }

            gameCacheService.putGameDetail(igdbId, fromDb)
            fromDb
        }
    }

    fun getVideoId(igdbId: Long): GameVideoResponse {
        return gameCacheService.getVideo(igdbId) ?: run {
            val fetched = fetchVideoId(igdbId)
            gameCacheService.putVideo(igdbId, fetched)
            fetched
        }
    }

    fun getSimilarGames(igdbId: Long): List<SimilarGameResponse> {
        return gameCacheService.getSimilarList(igdbId) ?: run {
            val recommendations = gameRecommendationService.getSimilarGames(igdbId, 10)
            val result = recommendations.map { r ->
                SimilarGameResponse(r.gameId().toLong(), r.name(), r.coverImageId())
            }
            gameCacheService.putSimilarList(igdbId, result)
            result
        }
    }

    private fun fetchVideoId(igdbId: Long): GameVideoResponse {
        val dto = igdbClient.getVideoId(igdbId)
        return if (dto?.videoId() == null) {
            GameVideoResponse.from("")
        } else {
            GameVideoResponse.from(dto.videoId())
        }
    }

    private fun assembleDetails(game: Game): GameDetailResponse {
        val genres = gameGenreRepository.findGenreNamesByGameId(game.id)
        val platforms = gamePlatformRepository.findPlatformNamesByGameId(game.id)
        val developers = gameCompanyRepository.findCompanyNamesByGameIdAndRole(game.id, CompanyRole.DEVELOPER)
        val publishers = gameCompanyRepository.findCompanyNamesByGameIdAndRole(game.id, CompanyRole.PUBLISHER)

        return GameDetailResponse.from(game, genres, platforms, developers, publishers)
    }

    private fun incrementViewCountInMemory(igdbId: Long) {
        gameCacheService.incrementViewCount(igdbId)
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    fun flushViewCountsToDb() {
        val snapshot = gameCacheService.getViewCountSnapshot()
        if (snapshot.isEmpty()) return

        log.info("조회수 DB 반영 시작: {} 건", snapshot.size)

        snapshot.forEach { (igdbId, counter) ->
            if (igdbId != null && counter != null) {
                val delta = counter.getAndSet(0)
                if (delta > 0) {
                    try {
                        gameRepository.incrementViewCount(igdbId, delta)
                    } catch (e: Exception) {
                        log.warn("조회수 반영 실패: igdbId=$igdbId, delta=$delta", e)
                        counter.addAndGet(delta)
                    }
                }
            }
        }
        log.info("조회수 DB 반영 완료")
    }

    fun findById(id: Int): Optional<Game> = gameRepository.findById(id)

    fun createGame(
        igdbId: Long,
        name: String,
        summary: String,
        coverImage: String?,
        firstReleaseDate: LocalDate?
    ): Game {
        // 1. LocalDate를 Long(초 단위)으로 변환
        val releaseTimestamp = firstReleaseDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toEpochSecond()

        // 2. 엔티티의 createGame에 정의된 9개의 파라미터를 모두 채워줍니다.
        val game = Game.createGame(
            igdbId = igdbId,
            name = name,
            summary = summary,
            imageId = coverImage,
            firstReleaseDate = releaseTimestamp, // 변환된 Long 전달
            storyline = null,        // 엔티티 정의에 있는 나머지 값들
            aggregatedRating = null,
            franchiseIgdbId = null,
            franchiseName = null
        )
        return gameRepository.save(game)
    }
}