package com.back.global.igdb

import com.back.global.igdb.dto.IgdbCompanyDto
import com.back.global.igdb.dto.IgdbGameDetailDto
import com.back.global.igdb.dto.IgdbGameModeDto
import com.back.global.igdb.dto.IgdbGenreDto
import com.back.global.igdb.dto.IgdbKeywordDto
import com.back.global.igdb.dto.IgdbPlatformDto
import com.back.global.igdb.dto.IgdbPlayerPerspectiveDto
import com.back.global.igdb.dto.IgdbThemeDto
import org.springframework.stereotype.Component
import kotlin.collections.toList

/**
 * 배치 전용 IGDB 클라이언트 (BatchIgdbRequestExecutor 사용).
 *
 * - connect 5s / read 30s 타임아웃 적용 → 500건 대용량 쿼리 안정 처리
 * - 사용처: IgdbGamePageReader, GenreSyncTasklet, PlatformSyncTasklet,
 *           ThemeSyncTasklet, GameModeSyncTasklet, PlayerPerspectiveSyncTasklet,
 *           KeywordSyncTasklet, CompanySyncTasklet
 *
 * API 경로(IgdbDefensiveClient → IgdbClient)와는 별도로 동작하며,
 * 동일한 @RateLimiter(name="igdb")를 통해 배치+API 합산 4 req/s 준수.
 */
@Component
class BatchIgdbClient(
    private val requestExecutor: BatchIgdbRequestExecutor,
) {
    companion object {
        private const val GAMES_ENDPOINT = "/games"
    }

    // ── 게임 동기화 (IgdbGamePageReader) ──────────────────────────────

    fun fetchGamePage(
        offset: Int,
        limit: Int,
    ): List<IgdbGameDetailDto> = fetchGamePage(offset, limit, null)

    fun fetchGamePage(
        offset: Int,
        limit: Int,
        updatedAfterEpoch: Long?,
    ): List<IgdbGameDetailDto> {
        val whereClause = if (updatedAfterEpoch != null) "where updated_at > $updatedAfterEpoch;" else ""

        val body =
            """
            fields id,name,summary,storyline,first_release_date,
                involved_companies.company.id,involved_companies.company.name,
                involved_companies.publisher,involved_companies.developer,
                cover.id,cover.image_id,
                genres.id,genres.name,
                platforms.id,platforms.name,
                themes.id,themes.name,
                keywords.id,keywords.name,
                game_modes.id,game_modes.name,
                player_perspectives.id,player_perspectives.name,
                external_games.external_game_source,external_games.uid,
                franchises.id,franchises.name,
                aggregated_rating,total_rating,total_rating_count;
            $whereClause
            sort id asc;
            offset $offset;
            limit $limit;
            """.trimIndent()

        val res = requestExecutor.execute(body, Array<IgdbGameDetailDto>::class.java, GAMES_ENDPOINT, "fetchGamePage")
        return res.toList()
    }

    // ── 마스터 데이터 동기화 (Tasklet 1~7) ────────────────────────────

    fun fetchGenres(): List<IgdbGenreDto> {
        val body =
            """
            fields id,name;
            limit 500;
            """.trimIndent()
        val res = requestExecutor.execute(body, Array<IgdbGenreDto>::class.java, "/genres", "fetchGenres")
        return res.toList()
    }

    fun fetchPlatforms(): List<IgdbPlatformDto> {
        val body =
            """
            fields id,name;
            limit 500;
            """.trimIndent()
        val res = requestExecutor.execute(body, Array<IgdbPlatformDto>::class.java, "/platforms", "fetchPlatforms")
        return res.toList()
    }

    fun fetchThemes(): List<IgdbThemeDto> {
        val body =
            """
            fields id,name;
            limit 500;
            """.trimIndent()
        val res = requestExecutor.execute(body, Array<IgdbThemeDto>::class.java, "/themes", "fetchThemes")
        return res.toList()
    }

    fun fetchGameModes(): List<IgdbGameModeDto> {
        val body =
            """
            fields id,name;
            limit 500;
            """.trimIndent()
        val res = requestExecutor.execute(body, Array<IgdbGameModeDto>::class.java, "/game_modes", "fetchGameModes")
        return res.toList()
    }

    fun fetchPlayerPerspectives(): List<IgdbPlayerPerspectiveDto> {
        val body =
            """
            fields id,name;
            limit 500;
            """.trimIndent()
        val res =
            requestExecutor.execute(
                body,
                Array<IgdbPlayerPerspectiveDto>::class.java,
                "/player_perspectives",
                "fetchPlayerPerspectives",
            )
        return res.toList()
    }

    fun fetchKeywords(): List<IgdbKeywordDto> = fetchAllPaged("/keywords", "fetchKeywords")

    fun fetchCompanies(): List<IgdbCompanyDto> = fetchAllPaged("/companies", "fetchCompanies")

    // ── Private helpers ───────────────────────────────────────────────

    /**
     * inline
     * 함수를 호출할 때 함수 호출 자체를 없애고, 함수 본문을 호출 위치에 펼쳐 넣는 최적화/기능 → reified를 가능하게 해줌
     *
     * reified T
     * 자바/코틀린 제네릭은 보통 런타임에 타입 정보가 지워진다. (type erasure)
     * reified T는 T::class.java를 얻을 수 있게 해줌
     * inline으로 함수가 호출 위치에 복사되면서, 컴파일러가 T를 실제 타입으로 치환할 수 있기 때문
     *
     * @Suppress("UNCHECKED_CAST") → Class<Array<T>> 캐스팅 경고를 조용히 처리함
     */
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> fetchAllPaged(
        endpoint: String,
        operationName: String,
    ): List<T> {
        // Array<T>::class.java 는 reified 치환 후에도 Object[].class 를 반환할 수 있어
        // Jackson 이 LinkedHashMap 으로 역직렬화하는 문제가 발생한다.
        // java.lang.reflect.Array.newInstance 를 통해 정확한 T[].class 를 얻는다.
        val arrayClass =
            java.lang.reflect.Array
                .newInstance(T::class.java, 0)
                .javaClass as Class<Array<T>>
        val all = mutableListOf<T>()
        var offset = 0
        val limit = 500

        while (true) {
            val body =
                """
                fields id,name;
                sort id asc;
                offset $offset;
                limit $limit;
                """.trimIndent()

            val res = requestExecutor.execute(body, arrayClass, endpoint, operationName)
            if (res.isEmpty()) break
            all.addAll(res.toList())
            if (res.size < limit) break
            offset += limit
        }

        return all
    }
}
