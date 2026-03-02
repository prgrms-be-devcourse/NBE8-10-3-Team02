package com.back.domain.game.recommendation.service

import com.back.domain.game.recommendation.dto.GameRecommendationResponse
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.repository.MemberVectorRepository
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameRecommendationService(
    private val gameVectorRepository: GameVectorRepository,
    private val memberVectorRepository: MemberVectorRepository,
    private val memberGameRepository: MemberGameRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CANDIDATE_MULTIPLIER = 3
    }

    @Transactional(readOnly = true)
    fun getPersonalRecommendations(
        memberId: Int,
        limit: Int,
    ): List<GameRecommendationResponse> {
        val profileVector = memberVectorRepository.getProfileVectorString(memberId)
        if (profileVector.isNullOrBlank()) {
            throw ServiceException("404-1", "프로필 벡터가 아직 생성되지 않았습니다. 라이브러리에 게임을 추가해주세요.")
        }

        // 보유 게임 제외 목록
        var ownedGameIds: List<Long> =
            memberGameRepository
                .findAllByMemberId(memberId)
                .mapNotNull { it.game.id }

        if (ownedGameIds.isEmpty()) {
            ownedGameIds = listOf(-1L) // native query IN clause에 빈 리스트 방지
        }

        val candidateLimit = minOf(limit * CANDIDATE_MULTIPLIER, 100)
        val results =
            gameVectorRepository.findSimilarGamesByUserVector(
                profileVector,
                ownedGameIds,
                candidateLimit,
            )

        return results
            .map { toRecommendationWithHybridScore(it) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    @Transactional(readOnly = true)
    fun getSimilarGames(
        igdbId: Long,
        limit: Int,
    ): List<GameRecommendationResponse> {
        val targetVector = gameVectorRepository.findFeatureVectorByIgdbId(igdbId)
            ?: return emptyList()
        val results = gameVectorRepository.findSimilarGamesByIgdbId(targetVector, igdbId, limit)
        return results
            .map { toRecommendationWithHybridScoreForIgdb(it) }
            .sortedByDescending { it.score }
    }

    /**
     * 하이브리드 스코어: 0.7×similarity + 0.2×(rating/100) + 0.1×(log(likeCount+1)/10)
     */
    private fun toRecommendationWithHybridScore(row: Array<Any?>): GameRecommendationResponse {
        val gameId = (row[0] as Number).toInt()
        val name = row[1] as? String
        val coverImageId = row[2] as? String
        val rating = (row[3] as? Number)?.toDouble() ?: 0.0
        val likeCount = (row[4] as? Number)?.toLong() ?: 0L
        val similarity = (row[5] as? Number)?.toDouble() ?: 0.0

        val ratingScore = rating / 100.0
        val popularityScore = Math.log1p(likeCount.toDouble()) / 10.0
        val hybridScore = 0.7 * similarity + 0.2 * ratingScore + 0.1 * popularityScore

        return GameRecommendationResponse(gameId, name, coverImageId, hybridScore, similarity)
    }

    /**
     * igdb_id를 반환하는 유사 게임 쿼리용 매핑
     */
    private fun toRecommendationWithHybridScoreForIgdb(row: Array<Any?>): GameRecommendationResponse {
        val igdbId = (row[0] as Number).toInt()
        val name = row[1] as? String
        val coverImageId = row[2] as? String
        val rating = (row[3] as? Number)?.toDouble() ?: 0.0
        val likeCount = (row[4] as? Number)?.toLong() ?: 0L
        val similarity = (row[5] as? Number)?.toDouble() ?: 0.0

        val ratingScore = rating / 100.0
        val popularityScore = Math.log1p(likeCount.toDouble()) / 10.0
        val hybridScore = 0.7 * similarity + 0.2 * ratingScore + 0.1 * popularityScore

        return GameRecommendationResponse(igdbId, name, coverImageId, hybridScore, similarity)
    }
}
