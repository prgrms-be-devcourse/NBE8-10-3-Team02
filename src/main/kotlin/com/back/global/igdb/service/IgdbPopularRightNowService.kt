package com.back.global.igdb.service

import com.back.global.igdb.IgdbDefensiveClient
import com.back.global.igdb.IgdbRequestExecutor
import com.back.global.igdb.dto.MultiQueryBlock
import com.back.global.igdb.dto.PopularGameCardDto
import com.back.global.igdb.dto.PopularityLists
import com.back.global.igdb.dto.PopularityPrimitiveRow
import com.back.global.igdb.dto.Weights
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.collections.isNullOrEmpty

@Service
class IgdbPopularRightNowService(
    private val igdbClient: IgdbDefensiveClient,
    private val requestExecutor: IgdbRequestExecutor,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // popularity_type ids
    companion object {
        private const val TYPE_VISITS = 1
        private const val TYPE_WANT = 2
        private const val TYPE_TWITCH_24H_WATCHED = 34
    }

    /**
     * 1. "Popular right now" = weighted score of (Visits, Want, watched).
     * 2. /multiquery로 한번에 3개의 popularity_primitives 가져오기
     * 3. 값을 [0..1]로 정규화 (log1p(value) / log1p(max)사용)
     * 4. score = wV*V + wW*W + wT*T
     * 5. Top N game_ids을 가지고, 인기게임 카드 정보 찾기
     */
    fun popularRightNow(topN: Int): List<PopularGameCardDto> {
        val weights = Weights(0.30, 0.20, 0.50)

        val candidateK = maxOf(300, topN * 50)

        val lists = fetchPrimitivesViaMultiquery(candidateK)

        // 1) Build normalized maps per type: gameId -> normScore(0..1)
        val visitsNorm = normalizeLog1p(lists.visits)
        val wantNorm = normalizeLog1p(lists.want)
        val twitchNorm = normalizeLog1p(lists.twitch)

        // 2) Union of all game ids
        val allGameIds = mutableSetOf<Long>()
        allGameIds.addAll(visitsNorm.keys)
        allGameIds.addAll(wantNorm.keys)
        allGameIds.addAll(twitchNorm.keys)

        if (allGameIds.isEmpty()) return emptyList()

        // 3) Weighted score
        val scoreByGameId = HashMap<Long, Double>(allGameIds.size * 2)
        for (gid in allGameIds) {
            val v = visitsNorm.getOrDefault(gid, 0.0)
            val w = wantNorm.getOrDefault(gid, 0.0)
            val t = twitchNorm.getOrDefault(gid, 0.0)

            val hasSupport = (w >= 0.06) || (t >= 0.06)
            if (!hasSupport) continue

            val score = weights.visits * v + weights.want * w + weights.twitch * t
            if (score > 0) scoreByGameId[gid] = score
        }

        // 4) Sort by score desc, take Top N
        val topIds =
            scoreByGameId.entries
                .sortedByDescending { it.value }
                .take(topN)
                .map { it.key }

        for (gid in topIds) {
            val v = visitsNorm.getOrDefault(gid, 0.0)
            val w = wantNorm.getOrDefault(gid, 0.0)
            val t = twitchNorm.getOrDefault(gid, 0.0)
            val s = scoreByGameId.getOrDefault(gid, 0.0)
            log.info("gid={} score={} (v={}, w={}, t={})", gid, s, v, w, t)
        }
        if (topIds.isEmpty()) return emptyList()

        // 5) Fetch games details
        val games = igdbClient.fetchGamesByIds(topIds)

        // 6) Join + keep score order
        val gameById = games.associateBy { it.id }

        val result = mutableListOf<PopularGameCardDto>()
        for (id in topIds) {
            val g = gameById[id] ?: continue
            val score = scoreByGameId.getOrDefault(id, 0.0)
            result.add(PopularGameCardDto.from(g, score))
        }

        return result
    }

    private fun fetchPrimitivesViaMultiquery(limitPerType: Int): PopularityLists {
        val body =
            """
            query popularity_primitives "visits" {
              fields game_id,value,popularity_type;
              where popularity_type = $TYPE_VISITS;
              sort value desc;
              limit $limitPerType;
            };
            query popularity_primitives "want" {
              fields game_id,value,popularity_type;
              where popularity_type = $TYPE_WANT;
              sort value desc;
              limit $limitPerType;
            };
            query popularity_primitives "twitch" {
              fields game_id,value,popularity_type;
              where popularity_type = $TYPE_TWITCH_24H_WATCHED;
              sort value desc;
              limit $limitPerType;
            };
            """.trimIndent()

        val raw = requestExecutor.execute(body, JsonNode::class.java, "/multiquery", "fetchPrimitivesViaMultiquery")

        if (!raw.isArray) return PopularityLists(emptyList(), emptyList(), emptyList())

        val blocks: List<MultiQueryBlock> =
            objectMapper.convertValue(
                raw,
                object : TypeReference<List<MultiQueryBlock>>() {},
            )

        val visits = extractBlock(blocks, "visits")
        val want = extractBlock(blocks, "want")
        val twitch = extractBlock(blocks, "twitch")

        return PopularityLists(visits, want, twitch)
    }

    private fun extractBlock(
        blocks: List<MultiQueryBlock>,
        name: String,
    ): List<PopularityPrimitiveRow> = blocks.firstOrNull { name.equals(it.name, ignoreCase = true) }?.result ?: emptyList()

    private fun normalizeLog1p(rows: List<PopularityPrimitiveRow>?): Map<Long, Double> {
        if (rows.isNullOrEmpty()) return emptyMap()

        var max = 0.0
        for (r in rows) {
            val v = safeDouble(r.value)
            if (v > max) max = v
        }
        if (max <= 0.0) return emptyMap()

        val denom = Math.log1p(max)

        val out = HashMap<Long, Double>(rows.size * 2)
        for (r in rows) {
            val gid = r.gameId
            val v = safeDouble(r.value)
            if (v <= 0) continue

            // norm in [0..1]
            var norm = Math.log1p(v) / denom
            // clamp just in case
            if (norm < 0) norm = 0.0
            if (norm > 1) norm = 1.0
            out[gid] = norm
        }
        return out
    }

    private fun safeDouble(bd: BigDecimal?): Double = bd?.toDouble() ?: 0.0
}
