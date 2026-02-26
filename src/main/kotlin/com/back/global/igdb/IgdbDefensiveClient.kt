package com.back.global.igdb

import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.repository.GameRepository
import com.back.global.igdb.dto.GameRow
import com.back.global.igdb.dto.IgdbCoverDto
import com.back.global.igdb.dto.IgdbGameNameDto
import com.back.global.igdb.dto.IgdbGameSummaryDto
import com.back.global.igdb.dto.IgdbGenreDto
import com.back.global.igdb.dto.IgdbPopularGameDto
import com.back.global.igdb.dto.IgdbPopularityPrimitiveDto
import com.back.global.igdb.dto.IgdbVideoDto
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.ZoneOffset

/**
 * Service → IgdbDefensiveClient → IgdbClient → IGDB API
 *           (서킷브레이커 + Bulkhead + RateLimiter + fallback)    (순수 HTTP)
 *
 * 방어 계층:
 *  - RateLimiter: 슬롯 없으면 200ms 후 즉시 DB fallback (사용자 줄세우기 방지)
 *  - Bulkhead: 동시 IGDB 호출을 최대 10개로 제한 → 나머지는 즉시 DB fallback
 *  - CircuitBreaker: 실패율 40% 초과 시 30초 OPEN → 즉시 DB fallback
 *  - Bulkhead 거부(BulkheadFullException)는 CB 실패로 집계하지 않음 (ignore-exceptions 설정)
 *
 * Aspect 실행 순서 (CB outer → Bulkhead → RateLimiter inner → IgdbClient):
 *  CB OPEN  → 즉시 fallback (Bulkhead, RateLimiter 진입 없음)
 *  CB CLOSED, Bulkhead FULL → BulkheadFullException → CB가 ignore → fallback 호출
 *  CB CLOSED, Bulkhead OK, RateLimiter 거부 → RateLimiter fallback 즉시 호출 (CB 미집계)
 *  CB CLOSED, Bulkhead OK, RateLimiter OK   → 실제 IGDB 호출
 */
@Component
class IgdbDefensiveClient(
    private val igdbClient: IgdbClient,
    private val gameRepository: GameRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "igdb", fallbackMethod = "searchGamesFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "searchGamesFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "searchGamesFallback")
    fun searchGames(
        keyword: String,
        limit: Int,
    ): List<IgdbGameSummaryDto> = igdbClient.searchGames(keyword, limit)

    @CircuitBreaker(name = "igdb", fallbackMethod = "getGameNameFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "getGameNameFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "getGameNameFallback")
    fun getGameName(igdbId: Long): IgdbGameNameDto? = igdbClient.getGameName(igdbId)

    @CircuitBreaker(name = "igdb", fallbackMethod = "getVideoIdFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "getVideoIdFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "getVideoIdFallback")
    fun getVideoId(igdbGameId: Long): IgdbVideoDto? = igdbClient.getVideoId(igdbGameId)

    @CircuitBreaker(name = "igdb", fallbackMethod = "getSimilarGameIdsFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "getSimilarGameIdsFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "getSimilarGameIdsFallback")
    fun getSimilarGameIds(igdbId: Long): List<Long> = igdbClient.getSimilarGameIds(igdbId)

    @CircuitBreaker(name = "igdb", fallbackMethod = "getSimilarGameBriefByIdFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "getSimilarGameBriefByIdFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "getSimilarGameBriefByIdFallback")
    fun getSimilarGameBriefById(ids: List<Long>): List<SimilarGameResponse> = igdbClient.getSimilarGameBriefById(ids)

    @CircuitBreaker(name = "igdb", fallbackMethod = "getPopularGameIdsFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "getPopularGameIdsFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "getPopularGameIdsFallback")
    fun getPopularGameIds(limit: Int): List<IgdbPopularityPrimitiveDto> = igdbClient.getPopularGameIds(limit)

    @CircuitBreaker(name = "igdb", fallbackMethod = "getGamesByIdsFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "getGamesByIdsFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "getGamesByIdsFallback")
    fun getGamesByIds(gameIds: List<Long>): List<IgdbPopularGameDto> = igdbClient.getGamesByIds(gameIds)

    @CircuitBreaker(name = "igdb", fallbackMethod = "fetchGamesByIdsFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "fetchGamesByIdsFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "fetchGamesByIdsFallback")
    fun fetchGamesByIds(idsInOrder: List<Long>): List<GameRow> = igdbClient.fetchGamesByIds(idsInOrder)

    @CircuitBreaker(name = "igdb", fallbackMethod = "getGameRatingFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "getGameRatingFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "getGameRatingFallback")
    fun getGameRating(igdbId: Long): IgdbPopularGameDto? = igdbClient.getGameRating(igdbId)

    @CircuitBreaker(name = "igdb", fallbackMethod = "fetchGenresFallback")
    @Bulkhead(name = "igdb", fallbackMethod = "fetchGenresFallback")
    @RateLimiter(name = "igdb", fallbackMethod = "fetchGenresFallback")
    fun fetchGenres(): List<IgdbGenreDto> = igdbClient.fetchGenres()

    // ── Fallback Methods ──
    // IGDB 장애, Bulkhead 초과, RateLimiter 거부 시 로컬 DB 데이터로 대체 응답

    private fun searchGamesFallback(
        keyword: String,
        limit: Int,
        t: Throwable,
    ): List<IgdbGameSummaryDto> {
        log.warn("IGDB fallback – searchGames(keyword={}), cause: {}", keyword, t.message)
        return gameRepository
            .findByNameContainingIgnoreCaseLimited(keyword, PageRequest.of(0, limit))
            .map { g ->
                IgdbGameSummaryDto(
                    g.igdbId,
                    g.name,
                    g.summary,
                    toEpochSeconds(g),
                    toCoverDto(g),
                    emptyList(),
                    emptyList(),
                )
            }
    }

    private fun getGameNameFallback(
        igdbId: Long,
        t: Throwable,
    ): IgdbGameNameDto? {
        log.warn("IGDB fallback – getGameName(igdbId={}), cause: {}", igdbId, t.message)
        return gameRepository
            .findByIgdbId(igdbId)
            .map { g -> IgdbGameNameDto(g.igdbId, g.name) }
            .orElse(null)
    }

    private fun getVideoIdFallback(
        igdbGameId: Long,
        t: Throwable,
    ): IgdbVideoDto? {
        log.warn("IGDB fallback – getVideoId(igdbId={}), cause: {}", igdbGameId, t.message)
        return null // 비디오 ID는 로컬 DB에 없으므로 null 유지 (호출부에서 빈 문자열 처리)
    }

    private fun getSimilarGameIdsFallback(
        igdbId: Long,
        t: Throwable,
    ): List<Long> {
        log.warn("IGDB fallback – getSimilarGameIds(igdbId={}), cause: {}", igdbId, t.message)
        return emptyList() // GameService.getSimilarGames()는 이미 pgvector를 직접 사용하므로 영향 없음
    }

    private fun getSimilarGameBriefByIdFallback(
        ids: List<Long>,
        t: Throwable,
    ): List<SimilarGameResponse> {
        log.warn("IGDB fallback – getSimilarGameBriefById, cause: {}", t.message)
        return gameRepository
            .findByIgdbIdIn(ids)
            .map { g -> SimilarGameResponse(g.igdbId, g.name, g.coverImageId) }
    }

    private fun getPopularGameIdsFallback(
        limit: Int,
        t: Throwable,
    ): List<IgdbPopularityPrimitiveDto> {
        log.warn("IGDB fallback – getPopularGameIds(limit={}), cause: {}", limit, t.message)
        return gameRepository
            .findByOrderByLikeCountDesc(PageRequest.of(0, limit))
            .map { g -> IgdbPopularityPrimitiveDto(0L, g.igdbId, g.likeCount.toDouble(), 1) }
    }

    private fun getGamesByIdsFallback(
        gameIds: List<Long>,
        t: Throwable,
    ): List<IgdbPopularGameDto> {
        log.warn("IGDB fallback – getGamesByIds, cause: {}", t.message)
        return gameRepository
            .findByIgdbIdIn(gameIds)
            .map { g ->
                IgdbPopularGameDto(
                    g.igdbId,
                    g.name,
                    toCoverDto(g),
                    g.aggregatedRating,
                    null,
                )
            }
    }

    private fun fetchGamesByIdsFallback(
        idsInOrder: List<Long>,
        t: Throwable,
    ): List<GameRow> {
        log.warn("IGDB fallback – fetchGamesByIds, cause: {}", t.message)
        val byIgdbId = gameRepository.findByIgdbIdIn(idsInOrder).associateBy { it.igdbId }
        return idsInOrder
            .mapNotNull { byIgdbId[it] }
            .map { g -> GameRow(g.igdbId, g.name, toCoverDto(g), emptyList()) }
    }

    private fun getGameRatingFallback(
        igdbId: Long,
        t: Throwable,
    ): IgdbPopularGameDto? {
        log.warn("IGDB fallback – getGameRating(igdbId={}), cause: {}", igdbId, t.message)
        return gameRepository
            .findByIgdbId(igdbId)
            .map { g ->
                IgdbPopularGameDto(
                    g.igdbId,
                    g.name,
                    toCoverDto(g),
                    g.aggregatedRating,
                    null,
                )
            }.orElse(null)
    }

    private fun fetchGenresFallback(t: Throwable): List<IgdbGenreDto> {
        log.warn("IGDB fallback – fetchGenres, cause: {}", t.message)
        return emptyList()
    }

    // ── Helper Methods ──

    private fun toCoverDto(g: Game): IgdbCoverDto? = g.coverImageId?.let { IgdbCoverDto(0L, it) }

    private fun toEpochSeconds(g: Game): Long? = g.firstReleaseDate?.atStartOfDay(ZoneOffset.UTC)?.toEpochSecond()
}
