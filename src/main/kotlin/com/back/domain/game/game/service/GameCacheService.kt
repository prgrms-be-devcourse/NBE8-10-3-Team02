package com.back.domain.game.game.service

import com.back.domain.game.game.dto.GameDetailResponse
import com.back.domain.game.game.dto.GameVideoResponse
import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.global.igdb.dto.PopularGameCardDto
import com.github.benmanes.caffeine.cache.Cache
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class GameCacheService(
    private val gameDetailCache: Cache<Long, GameDetailResponse>,
    private val videoIdCache: Cache<Long, GameVideoResponse>,
    private val similarIdsCache: Cache<Long, List<Long>>,
    private val similarListCache: Cache<Long, List<SimilarGameResponse>>,
    private val igdbPopularGamesCache: Cache<String, List<PopularGameCardDto>>,
    private val viewCountCache: Cache<Long, AtomicLong>,
) {
    // GameDetail
    fun getGameDetail(igdbId: Long): GameDetailResponse? = gameDetailCache.getIfPresent(igdbId)

    fun putGameDetail(
        igdbId: Long,
        response: GameDetailResponse,
    ) = gameDetailCache.put(igdbId, response)

    // Video
    fun getVideo(igdbId: Long): GameVideoResponse? = videoIdCache.getIfPresent(igdbId)

    fun putVideo(
        igdbId: Long,
        response: GameVideoResponse,
    ) = videoIdCache.put(igdbId, response)

    // Similar IDs
    fun getSimilarIds(igdbId: Long): List<Long>? = similarIdsCache.getIfPresent(igdbId)

    fun putSimilarIds(
        igdbId: Long,
        ids: List<Long>,
    ) = similarIdsCache.put(igdbId, ids)

    // Similar List
    fun getSimilarList(igdbId: Long): List<SimilarGameResponse>? = similarListCache.getIfPresent(igdbId)

    fun putSimilarList(
        igdbId: Long,
        list: List<SimilarGameResponse>,
    ) = similarListCache.put(igdbId, list)

    // IGDB Popular Games
    fun getIgdbPopularGames(key: String): List<PopularGameCardDto>? = igdbPopularGamesCache.getIfPresent(key)

    fun putIgdbPopularGames(
        key: String,
        list: List<PopularGameCardDto>,
    ) = igdbPopularGamesCache.put(key, list)

    // View Count
    fun incrementViewCount(igdbId: Long) {
        viewCountCache.get(igdbId) { AtomicLong(0) }.incrementAndGet()
    }

    fun getViewCountSnapshot(): Map<Long, AtomicLong> = HashMap(viewCountCache.asMap())
}
