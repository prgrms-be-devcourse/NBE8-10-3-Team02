package com.back.domain.game.recommendation.service

import com.back.domain.game.gameLike.repository.GameLikeRepository
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.repository.MemberVectorRepository
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.entity.MemberGame
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.global.vector.VectorDimensionMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileVectorService(
    private val memberGameRepository: MemberGameRepository,
    private val gameLikeRepository: GameLikeRepository,
    private val gameVectorRepository: GameVectorRepository,
    private val memberVectorRepository: MemberVectorRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun calculateAndSaveProfileVector(memberId: Int) {
        val memberGames = memberGameRepository.findAllByMemberId(memberId)
        if (memberGames.isEmpty()) {
            memberVectorRepository.updateProfileVector(memberId, null)
            return
        }

        // DB에 저장된 feature_vector를 한 번의 쿼리로 조회 (기존: 게임당 5쿼리 → 1쿼리)
        val gameVectorMap = loadFeatureVectors(memberId)

        val likedGameIds = gameLikeRepository.findGameIdsByMemberId(memberId).toSet()

        val profileVector = FloatArray(VectorDimensionMapper.TOTAL_DIMENSIONS)
        var totalWeight = 0.0

        for (mg in memberGames) {
            val gameVector = gameVectorMap[mg.game.id] ?: continue

            val weight = calculateWeight(mg, likedGameIds.contains(mg.game.id))

            for (i in 0 until VectorDimensionMapper.TOTAL_DIMENSIONS) {
                profileVector[i] += (gameVector[i] * weight).toFloat()
            }
            totalWeight += weight
        }

        // 가중 평균
        if (totalWeight > 0) {
            for (i in 0 until VectorDimensionMapper.TOTAL_DIMENSIONS) {
                profileVector[i] /= totalWeight.toFloat()
            }
        }

        val vectorString = GameVectorService.vectorToString(profileVector)
        memberVectorRepository.updateProfileVector(memberId, vectorString)
        log.debug("프로필 벡터 갱신 완료: memberId={}", memberId)
    }

    private fun loadFeatureVectors(memberId: Int): Map<Long?, FloatArray> {
        val rows = gameVectorRepository.findFeatureVectorsByMemberId(memberId)
        val map = HashMap<Long?, FloatArray>(rows.size)
        for (row in rows) {
            val gameId = (row[0] as Number).toLong()
            val vectorStr = row[1] as String
            map[gameId] = parseVector(vectorStr)
        }
        return map
    }

    private fun calculateWeight(
        mg: MemberGame,
        isLiked: Boolean,
    ): Double {
        var weight = 1.0

        // 즐겨찾기 가중치
        if (mg.isFavorite) weight *= 2.5

        // 상태 기반 가중치
        weight *= getStatusWeight(mg.status)

        // 플레이타임 가중치 (log scale)
        if (mg.playtime > 0) weight *= (1.0 + Math.log1p(mg.playtime) * 0.3)

        // 좋아요 가중치
        if (isLiked) weight *= 1.3

        // 리뷰 평점 가중치
        val review = mg.review
        if (review != null && review.rating > 0) {
            weight *= (review.rating / 5.0)
        }

        return weight
    }

    private fun getStatusWeight(status: StatusEnum?): Double =
        when (status) {
            StatusEnum.COMPLETED -> 2.0
            StatusEnum.PLAYING -> 1.8
            StatusEnum.ON_HOLD -> 1.2
            StatusEnum.PLAN_TO_PLAY -> 0.8
            StatusEnum.DROPPED -> 0.5
            null -> 1.0
        }

    companion object {
        private fun parseVector(vectorStr: String): FloatArray {
            // pgvector format: "[0.0,1.0,0.0,...]"
            val inner = vectorStr.substring(1, vectorStr.length - 1)
            val parts = inner.split(",")
            return FloatArray(parts.size) { parts[it].toFloat() }
        }
    }
}
