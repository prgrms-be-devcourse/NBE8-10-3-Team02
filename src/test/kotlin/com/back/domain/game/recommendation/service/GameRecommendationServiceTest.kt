package com.back.domain.game.recommendation.service

import com.back.domain.game.GameTestFixtures
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.repository.MemberVectorRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.memberGame.entity.MemberGame
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class GameRecommendationServiceTest {
    @Mock lateinit var gameVectorRepository: GameVectorRepository

    @Mock lateinit var memberVectorRepository: MemberVectorRepository

    @Mock lateinit var memberGameRepository: MemberGameRepository

    @InjectMocks
    private lateinit var service: GameRecommendationService

    companion object {
        private const val MEMBER_ID = 1
    }

    @Nested
    inner class `getPersonalRecommendations` {
        @Test
        fun `프로필 벡터가 null이면 ServiceException 발생`() {
            whenever(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn(null)

            assertThatThrownBy { service.getPersonalRecommendations(MEMBER_ID, 20) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessageContaining("프로필 벡터가 아직 생성되지 않았습니다")
        }

        @Test
        fun `프로필 벡터가 빈 문자열이면 ServiceException 발생`() {
            whenever(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("   ")

            assertThatThrownBy { service.getPersonalRecommendations(MEMBER_ID, 20) }
                .isInstanceOf(ServiceException::class.java)
        }

        @Test
        fun `보유 게임이 없으면 excludeGameIds에 -1을 넣어 빈 IN clause 방지`() {
            whenever(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0,0.0]")
            whenever(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(listOf())
            whenever(gameVectorRepository.findSimilarGamesByUserVector(any(), any(), any()))
                .thenReturn(listOf())

            service.getPersonalRecommendations(MEMBER_ID, 20)

            verify(gameVectorRepository).findSimilarGamesByUserVector(
                eq("[1.0,0.0]"),
                eq(listOf(-1L)),
                eq(60), // 20 × 3
            )
        }

        @Test
        fun `보유 게임 id들을 exclude 목록에 포함한다`() {
            val game1 = GameTestFixtures.gameWithId(10L, 100L, "G1", "")
            val game2 = GameTestFixtures.gameWithId(20L, 200L, "G2", "")
            val member = Member(1, "test@test.com", "tester")
            val mg1 = MemberGame(1L, 0.0, false, null, member, game1)
            val mg2 = MemberGame(1L, 0.0, false, null, member, game2)

            whenever(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0]")
            whenever(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(listOf(mg1, mg2))
            whenever(gameVectorRepository.findSimilarGamesByUserVector(any(), any(), any()))
                .thenReturn(listOf())

            service.getPersonalRecommendations(MEMBER_ID, 10)

            verify(gameVectorRepository).findSimilarGamesByUserVector(
                any(),
                eq(listOf(10L, 20L)),
                eq(30), // 10 × 3
            )
        }

        @Test
        fun `candidateLimit는 limit×3이고 최대 100이다`() {
            whenever(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0]")
            whenever(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(listOf())
            whenever(gameVectorRepository.findSimilarGamesByUserVector(any(), any(), any()))
                .thenReturn(listOf())

            // limit=50 → 50×3=150 → min(150,100) = 100
            service.getPersonalRecommendations(MEMBER_ID, 50)

            verify(gameVectorRepository).findSimilarGamesByUserVector(
                any(),
                any(),
                eq(100),
            )
        }

        @Test
        fun `하이브리드 스코어로 재정렬하여 limit개만 반환한다`() {
            whenever(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0]")
            whenever(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(listOf())

            // row: [id, name, coverImageId, rating, likeCount, similarity]
            val candidates =
                listOf(
                    row(1, "HighSim", null, 0.0, 0L, 0.95), // 0.7×0.95 = 0.665
                    row(2, "HighRate", null, 100.0, 0L, 0.80), // 0.7×0.80 + 0.2×1.0 = 0.76
                    row(3, "Popular", null, 50.0, 10000L, 0.70), // 0.7×0.70 + 0.2×0.5 + 0.1×log1p(10000)/10
                )
            whenever(gameVectorRepository.findSimilarGamesByUserVector(any(), any(), any()))
                .thenReturn(candidates)

            val results = service.getPersonalRecommendations(MEMBER_ID, 2)

            assertThat(results).hasSize(2)
            assertThat(results[0].score).isGreaterThanOrEqualTo(results[1].score)
        }
    }

    @Nested
    inner class `getSimilarGames` {
        @Test
        fun `유사 게임 결과를 하이브리드 스코어 내림차순으로 반환한다`() {
            val igdbId = 999L
            val targetVector = "[1.0,0.0]"
            val rows =
                listOf(
                    row(1, "GameA", "cover1", 90.0, 500L, 0.85),
                    row(2, "GameB", "cover2", 60.0, 100L, 0.90),
                )
            whenever(gameVectorRepository.findFeatureVectorByIgdbId(igdbId)).thenReturn(targetVector)
            whenever(gameVectorRepository.findSimilarGamesByIgdbId(targetVector, igdbId, 10)).thenReturn(rows)

            val results = service.getSimilarGames(igdbId, 10)

            assertThat(results).hasSize(2)
            assertThat(results[0].score).isGreaterThanOrEqualTo(results[1].score)
        }

        @Test
        fun `벡터가 없으면 빈 리스트를 반환한다`() {
            whenever(gameVectorRepository.findFeatureVectorByIgdbId(999L)).thenReturn(null)

            val results = service.getSimilarGames(999L, 10)

            assertThat(results).isEmpty()
        }
    }

    @Nested
    inner class `하이브리드 스코어 계산` {
        private val targetVector = "[1.0,0.0]"

        @Test
        fun `공식 - 0_7×similarity + 0_2×(rating÷100) + 0_1×(log1p(likeCount)÷10)`() {
            val similarity = 0.85
            val rating = 80.0
            val likeCount = 100L

            val rows = listOf(row(1, "Test", "cover", rating, likeCount, similarity))
            whenever(gameVectorRepository.findFeatureVectorByIgdbId(1L)).thenReturn(targetVector)
            whenever(gameVectorRepository.findSimilarGamesByIgdbId(targetVector, 1L, 10)).thenReturn(rows)

            val results = service.getSimilarGames(1L, 10)

            val expected = 0.7 * similarity + 0.2 * (rating / 100.0) + 0.1 * (Math.log1p(likeCount.toDouble()) / 10.0)

            assertThat(results[0].score).isCloseTo(expected, within(0.0001))
            assertThat(results[0].similarity).isCloseTo(similarity, within(0.0001))
        }

        @Test
        fun `rating이 null이면 0으로 처리한다`() {
            val similarity = 0.9
            val rows = listOf(rowWithNulls(1, "Test", null, null, 0L, similarity))
            whenever(gameVectorRepository.findFeatureVectorByIgdbId(1L)).thenReturn(targetVector)
            whenever(gameVectorRepository.findSimilarGamesByIgdbId(targetVector, 1L, 10)).thenReturn(rows)

            val results = service.getSimilarGames(1L, 10)

            val expected = 0.7 * similarity + 0.2 * 0.0 + 0.1 * (Math.log1p(0.0) / 10.0)
            assertThat(results[0].score).isCloseTo(expected, within(0.0001))
        }

        @Test
        fun `likeCount가 null이면 0으로 처리한다`() {
            val similarity = 0.9
            val rows = listOf(rowWithNulls(1, "Test", null, 80.0, null, similarity))
            whenever(gameVectorRepository.findFeatureVectorByIgdbId(1L)).thenReturn(targetVector)
            whenever(gameVectorRepository.findSimilarGamesByIgdbId(targetVector, 1L, 10)).thenReturn(rows)

            val results = service.getSimilarGames(1L, 10)

            val expected = 0.7 * similarity + 0.2 * 0.8 + 0.1 * 0.0
            assertThat(results[0].score).isCloseTo(expected, within(0.0001))
        }

        @Test
        fun `similarity가 높을수록 하이브리드 스코어에 가장 큰 영향을 준다 (가중치 0_7)`() {
            val rows =
                listOf(
                    row(1, "HighSim", null, 50.0, 50L, 0.95),
                    row(2, "LowSim", null, 50.0, 50L, 0.30),
                )
            whenever(gameVectorRepository.findFeatureVectorByIgdbId(1L)).thenReturn(targetVector)
            whenever(gameVectorRepository.findSimilarGamesByIgdbId(targetVector, 1L, 10)).thenReturn(rows)

            val results = service.getSimilarGames(1L, 10)

            val highSim = results.first { it.name == "HighSim" }
            val lowSim = results.first { it.name == "LowSim" }
            val scoreDiff = highSim.score - lowSim.score
            val simContribDiff = 0.7 * (0.95 - 0.30)
            assertThat(scoreDiff).isCloseTo(simContribDiff, within(0.0001))
        }

        @Test
        fun `similarity 동점일 때 rating이 높으면 역전한다`() {
            val rows =
                listOf(
                    row(1, "LowRate", null, 20.0, 0L, 0.80),
                    row(2, "HighRate", null, 100.0, 0L, 0.80),
                )
            whenever(gameVectorRepository.findFeatureVectorByIgdbId(1L)).thenReturn(targetVector)
            whenever(gameVectorRepository.findSimilarGamesByIgdbId(targetVector, 1L, 10)).thenReturn(rows)

            val results = service.getSimilarGames(1L, 10)

            assertThat(results[0].name).isEqualTo("HighRate")
        }
    }

    // --- 팩토리 헬퍼 ---

    private fun row(
        id: Int,
        name: String,
        coverImageId: String?,
        rating: Double,
        likeCount: Long,
        similarity: Double,
    ): Array<Any?> = arrayOf(id, name, coverImageId, rating, likeCount, similarity)

    private fun rowWithNulls(
        id: Int,
        name: String,
        coverImageId: String?,
        rating: Double?,
        likeCount: Long?,
        similarity: Double,
    ): Array<Any?> = arrayOf(id, name, coverImageId, rating, likeCount, similarity)
}
