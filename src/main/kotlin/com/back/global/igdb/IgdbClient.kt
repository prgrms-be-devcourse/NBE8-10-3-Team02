package com.back.global.igdb

import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.global.igdb.dto.GameRow
import com.back.global.igdb.dto.IgdbGameBriefDto
import com.back.global.igdb.dto.IgdbGameDetailDto
import com.back.global.igdb.dto.IgdbGameNameDto
import com.back.global.igdb.dto.IgdbGameSummaryDto
import com.back.global.igdb.dto.IgdbGenreDto
import com.back.global.igdb.dto.IgdbPopularGameDto
import com.back.global.igdb.dto.IgdbPopularityPrimitiveDto
import com.back.global.igdb.dto.IgdbSimilarIdsDto
import com.back.global.igdb.dto.IgdbVideoDto
import org.springframework.stereotype.Component
import kotlin.collections.isEmpty
import kotlin.collections.map
import kotlin.collections.toList

/**
 * API 경로 전용 IGDB 클라이언트 (IgdbRequestExecutor 사용).
 * - connect 1s / read 3s 타임아웃 → 빠른 실패 → CB open 가속 → DB fallback
 * - 사용처: IgdbDefensiveClient (사용자 요청 경로)
 * 배치 경로는 BatchIgdbClient를 사용.
 */
@Component
class IgdbClient(
    private val requestExecutor: IgdbRequestExecutor,
) {
    companion object {
        private const val GAMES_ENDPOINT = "/games"
        private const val GAME_VIDEOS_ENDPOINT = "/game_videos"
    }

    fun searchGames(
        keyword: String,
        limit: Int,
    ): List<IgdbGameSummaryDto> {
        val body =
            """
            search "${escape(keyword)}";
            fields id,name,summary,first_release_date;
            limit $limit;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbGameSummaryDto>::class.java, GAMES_ENDPOINT, "searchGames")
        return res.toList()
    }

    fun getGameDetail(igdbId: Long): IgdbGameDetailDto? {
        val body =
            """
            fields
                id,name,summary,first_release_date,
                involved_companies.company.name,
                involved_companies.publisher,
                involved_companies.developer,
                cover.id,cover.image_id,
                genres.id,genres.name,
                platforms.id,platforms.name;
            where id = $igdbId;
            limit 1;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbGameDetailDto>::class.java, GAMES_ENDPOINT, "getGameDetail")
        return res.firstOrNull()
    }

    fun getGameName(igdbId: Long): IgdbGameNameDto? {
        val body =
            """
            fields
                id,name;
            where id = $igdbId;
            limit 1;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbGameNameDto>::class.java, GAMES_ENDPOINT, "getGameName")
        return res.firstOrNull()
    }

    fun getVideoId(igdbGameId: Long): IgdbVideoDto? {
        val body =
            """
            fields
                video_id;
            where game = $igdbGameId;
            sort id desc;
            limit 1;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbVideoDto>::class.java, GAME_VIDEOS_ENDPOINT, "getVideoId")
        return res.firstOrNull()
    }

    fun getSimilarGameIds(igdbId: Long): List<Long> {
        val body =
            """
            fields
                similar_games;
            where id = $igdbId;
            limit 1;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbSimilarIdsDto>::class.java, GAMES_ENDPOINT, "getSimilarGameIds")
        return res.firstOrNull()?.similarGames ?: emptyList()
    }

    fun getSimilarGameBriefById(ids: List<Long>): List<SimilarGameResponse> {
        val picked = ids.take(30)
        if (picked.isEmpty()) return emptyList()

        val idList = picked.joinToString(",")
        val body =
            """
            fields id, name, cover.image_id;
            where id = ($idList);
            limit ${picked.size};
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbGameBriefDto>::class.java, GAMES_ENDPOINT, "fetchGameBriefsByIds")
        if (res.isEmpty()) return emptyList()

        return res.map { d -> SimilarGameResponse(d.id, d.name, d.cover?.imageId) }
    }

    fun getPopularGameIds(limit: Int): List<IgdbPopularityPrimitiveDto> {
        val body =
            """
            fields game_id,value,popularity_type;
            where popularity_type = 1;
            sort value desc;
            limit $limit;
            """.trimIndent()

        val res =
            requestExecutor.execute(
                body,
                Array<IgdbPopularityPrimitiveDto>::class.java,
                "/popularity_primitives",
                "getPopularGameIds",
            )
        return res.toList()
    }

    fun getGamesByIds(gameIds: List<Long>): List<IgdbPopularGameDto> {
        if (gameIds.isEmpty()) return emptyList()

        val idList = gameIds.joinToString(",")
        val body =
            """
            fields id, name, cover.image_id, total_rating, total_rating_count;
            where id = ($idList);
            limit ${gameIds.size};
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbPopularGameDto>::class.java, GAMES_ENDPOINT, "getGamesByIds")
        return res.toList()
    }

    fun fetchGamesByIds(idsInOrder: List<Long>): List<GameRow> {
        val idList = idsInOrder.joinToString(",")
        val body =
            """
            fields id,name,cover.image_id,genres.id,genres.name,platforms.id,platforms.name,first_release_date;
            where id = ($idList);
            limit ${idsInOrder.size};
            """.trimIndent()

        val games = requestExecutor.execute(body, Array<GameRow>::class.java, GAMES_ENDPOINT, "fetchGamesByIds")
        return games.toList()
    }

    fun getGameRating(igdbId: Long): IgdbPopularGameDto? {
        val body =
            """
            fields id, name, cover.image_id, total_rating, total_rating_count;
            where id = $igdbId;
            limit 1;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbPopularGameDto>::class.java, GAMES_ENDPOINT, "getGameRating")
        return res.firstOrNull()
    }

    fun fetchGenres(): List<IgdbGenreDto> {
        val body =
            """
            fields id,name;
            limit 500;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbGenreDto>::class.java, "/genres", "fetchGenres")
        return res.toList()
    }

    private fun escape(s: String) = s.replace("\"", "\\\"")
}
