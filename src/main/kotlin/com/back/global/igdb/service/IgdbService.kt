package com.back.global.igdb.service

import com.back.domain.game.game.dto.GameSearchCondition
import com.back.global.igdb.IgdbDefensiveClient
import com.back.global.igdb.IgdbProperties
import com.back.global.igdb.TwitchTokenService
import com.back.global.igdb.dto.IgdbGameSummaryDto
import com.back.global.igdb.dto.IgdbGenreDto
import com.back.global.igdb.dto.IgdbPlatformDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

@Service
class IgdbService(
    private val restTemplate: RestTemplate,
    private val twitchTokenService: TwitchTokenService,
    private val props: IgdbProperties,
    private val igdbClient: IgdbDefensiveClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(condition: GameSearchCondition): List<IgdbGameSummaryDto> {
        val query = condition.query
        val genreIds = condition.genreIds
        val platformIds = condition.platformIgdbIds

        val size = condition.size ?: 20
        val page = condition.page ?: 1
        val offset = (page - 1) * size

        //        검색결과 없으면 종료
        if (query.isNullOrBlank()) {
            return emptyList()
        }

        val body = buildIgdbQuery(query, genreIds, platformIds, size, offset)

        val headers =
            HttpHeaders().apply {
                set("Client-ID", props.clientId)
                setBearerAuth(twitchTokenService.getAccessToken())
                contentType = MediaType.TEXT_PLAIN
            }

        val request = HttpEntity(body, headers)

        //        API 호출
        return try {
            val response =
                restTemplate.postForEntity(
                    "https://api.igdb.com/v4/games",
                    request,
                    Array<IgdbGameSummaryDto>::class.java,
                )

            (response.body ?: emptyArray())
                .filter { d -> d.name != null && d.name.lowercase().contains(query.lowercase()) }
        } catch (e: HttpStatusCodeException) {
            log.error("IGDB error (status={})", e.statusCode, e)
            emptyList()
        }
    }

    /**
     * IGDB 쿼리 생성 책임 분리
     */
    private fun buildIgdbQuery(
        query: String,
        genreIds: List<Long>?,
        platformIds: List<Long>?,
        size: Int,
        offset: Int,
    ): String {
        val sb = StringBuilder()

        // 'search' 대신 'fields'를 먼저 선언
        sb.append("fields id, name, first_release_date, cover.image_id, genres, platforms;\n")

        val whereConditions = mutableListOf<String>()

        // 1. 이름 검색 (Case-insensitive matching)
        if (!query.isBlank()) {
            whereConditions.add("name ~ *\"$query\"*")
        }

        // 2. 장르 필터
        if (!genreIds.isNullOrEmpty()) {
            whereConditions.add("genres = (${genreIds.joinToString(",")})")
        }

        // 3. 플랫폼 필터
        if (!platformIds.isNullOrEmpty()) {
            whereConditions.add("platforms = (${platformIds.joinToString(",")})")
        }

        if (whereConditions.isNotEmpty()) {
            sb.append("where ")
            sb.append(whereConditions.joinToString(" & "))
            sb.append(";\n")
        }

        // 정렬 추가
        sb.append("sort first_release_date desc;\n")

        // 페이지네이션
        sb.append("limit $size; ")
        sb.append("offset $offset;")

        return sb.toString()
    }

    fun getGenres(): List<IgdbGenreDto> = igdbClient.fetchGenres()

    fun getPlatformNameMap(platformIds: Set<Long>?): Map<Long, String> {
        if (platformIds.isNullOrEmpty()) return emptyMap()

        val platformCondition = platformIds.joinToString(",")
        val body =
            """
            fields id,name;
            where id = ($platformCondition);
            """.trimIndent()

        val headers =
            HttpHeaders().apply {
                set("Client-ID", props.clientId)
                setBearerAuth(twitchTokenService.getAccessToken())
                contentType = MediaType.TEXT_PLAIN
            }

        val request = HttpEntity(body, headers)

        return try {
            val response =
                restTemplate.postForEntity(
                    "https://api.igdb.com/v4/platforms",
                    request,
                    Array<IgdbPlatformDto>::class.java,
                )
            (response.body ?: emptyArray()).associate { it.id to (it.name ?: "") }
        } catch (e: HttpStatusCodeException) {
            log.error("IGDB platform error", e)
            emptyMap()
        }
    }
}
