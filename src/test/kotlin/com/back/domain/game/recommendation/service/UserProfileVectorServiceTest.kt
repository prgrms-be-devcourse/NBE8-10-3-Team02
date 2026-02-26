package com.back.domain.game.recommendation.service

import com.back.domain.game.GameTestFixtures
import com.back.domain.game.game.entity.Game
import com.back.domain.game.gameLike.repository.GameLikeRepository
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.repository.MemberVectorRepository
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.entity.MemberGame
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.domain.review.entity.Review
import com.back.global.vector.VectorDimensionMapper.TOTAL_DIMENSIONS
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class UserProfileVectorServiceTest {
    @Mock lateinit var memberGameRepository: MemberGameRepository

    @Mock lateinit var gameLikeRepository: GameLikeRepository

    @Mock lateinit var gameVectorRepository: GameVectorRepository

    @Mock lateinit var memberVectorRepository: MemberVectorRepository

    @InjectMocks
    private lateinit var service: UserProfileVectorService

    companion object {
        private const val MEMBER_ID = 1
    }

    @Nested
    inner class `calculateAndSaveProfileVector` {
        @Test
        fun `라이브러리가 비어있으면 null 벡터를 저장한다`() {
            whenever(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(listOf())

            service.calculateAndSaveProfileVector(MEMBER_ID)

            verify(memberVectorRepository).updateProfileVector(MEMBER_ID, null)
            verify(gameVectorRepository, never()).findFeatureVectorsByMemberId(any())
            verifyNoInteractions(gameLikeRepository)
            verifyNoMoreInteractions(memberVectorRepository, gameVectorRepository)
        }

        @Test
        fun `게임 1개만 보유 시 프로필 벡터 = 해당 게임 벡터`() {
            val game = gameWithId(1)
            val mg = memberGame(game, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mg))
            stubFeatureVectors(arrayOf(1, vectorString(1.0f, 0.0f)))
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            assertThat(saved[0]).isEqualTo(1.0f)
            assertThat(saved[1]).isEqualTo(0.0f)
        }

        @Test
        fun `feature_vector가 없는 게임은 건너뛴다`() {
            val game1 = gameWithId(1)
            val game2 = gameWithId(2)
            val mg1 = memberGame(game1, StatusEnum.PLAYING, false, 0.0, null)
            val mg2 = memberGame(game2, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mg1, mg2))
            // game1만 벡터 있음, game2는 없음
            stubFeatureVectors(arrayOf(1, vectorString(1.0f, 0.0f)))
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            assertThat(saved[0]).isEqualTo(1.0f)
            assertThat(saved[1]).isEqualTo(0.0f)
        }

        @Test
        fun `모든 게임의 feature_vector가 없으면 영벡터를 저장한다`() {
            val game = gameWithId(1)
            val mg = memberGame(game, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mg))
            stubFeatureVectors() // 빈 결과
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            for (v in saved) {
                assertThat(v).isEqualTo(0.0f)
            }
        }
    }

    @Nested
    inner class `가중치 계산 검증` {
        @Test
        fun `동일 벡터 2개 게임의 가중치가 다르면 결과는 동일 벡터`() {
            val game1 = gameWithId(1)
            val game2 = gameWithId(2)
            val mg1 = memberGame(game1, StatusEnum.COMPLETED, true, 100.0, null) // 높은 가중치
            val mg2 = memberGame(game2, StatusEnum.DROPPED, false, 0.0, null) // 낮은 가중치

            stubLibrary(listOf(mg1, mg2))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(1.0f, 0.0f)),
            )
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            assertThat(saved[0]).isEqualTo(1.0f)
            assertThat(saved[1]).isEqualTo(0.0f)
        }

        @Test
        fun `서로 다른 벡터 2개 → 가중치 비율에 따라 가중 평균`() {
            val gameA = gameWithId(1)
            val gameB = gameWithId(2)
            val mgA = memberGame(gameA, StatusEnum.COMPLETED, false, 0.0, null)
            val mgB = memberGame(gameB, StatusEnum.DROPPED, false, 0.0, null)

            stubLibrary(listOf(mgA, mgB))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(0.0f, 1.0f)),
            )
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            // weight A = 1.0 × 2.0 = 2.0, weight B = 1.0 × 0.5 = 0.5, total = 2.5
            assertThat(saved[0]).isCloseTo(0.8f, within(0.001f))
            assertThat(saved[1]).isCloseTo(0.2f, within(0.001f))
        }

        @Test
        fun `즐겨찾기 → 가중치에 2_5 곱셈`() {
            val gameA = gameWithId(1)
            val gameB = gameWithId(2)
            val mgA = memberGame(gameA, StatusEnum.PLAYING, true, 0.0, null)
            val mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mgA, mgB))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(0.0f, 1.0f)),
            )
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            val wA = 2.5 * 1.8 // 4.5
            val wB = 1.8
            val total = wA + wB // 6.3
            assertThat(saved[0]).isCloseTo((wA / total).toFloat(), within(0.001f))
            assertThat(saved[1]).isCloseTo((wB / total).toFloat(), within(0.001f))
        }

        @ParameterizedTest
        @CsvSource(
            "COMPLETED, 2.0",
            "PLAYING, 1.8",
            "ON_HOLD, 1.2",
            "PLAN_TO_PLAY, 0.8",
            "DROPPED, 0.5",
        )
        fun `상태별 가중치 검증`(
            status: StatusEnum,
            expectedWeight: Double,
        ) {
            val gameA = gameWithId(1)
            val gameB = gameWithId(2)
            val mgA = memberGame(gameA, status, false, 0.0, null)
            val mgB = memberGame(gameB, StatusEnum.COMPLETED, false, 0.0, null) // weight=2.0 기준

            stubLibrary(listOf(mgA, mgB))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(0.0f, 1.0f)),
            )
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            val total = expectedWeight + 2.0
            assertThat(saved[0]).isCloseTo((expectedWeight / total).toFloat(), within(0.001f))
            assertThat(saved[1]).isCloseTo((2.0 / total).toFloat(), within(0.001f))
        }

        @Test
        @DisplayName("플레이타임 > 0 → (1 + log1p(playtime) × 0_3) 가중치")
        fun funLog1() {
            val playtime = 100.0
            val playtimeWeight = 1.0 + Math.log1p(playtime) * 0.3
            val gameA = gameWithId(1)
            val gameB = gameWithId(2)
            val mgA = memberGame(gameA, StatusEnum.PLAYING, false, playtime, null)
            val mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mgA, mgB))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(0.0f, 1.0f)),
            )
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            val wA = 1.8 * playtimeWeight
            val wB = 1.8
            val total = wA + wB
            assertThat(saved[0]).isCloseTo((wA / total).toFloat(), within(0.001f))
            assertThat(saved[1]).isCloseTo((wB / total).toFloat(), within(0.001f))
        }

        @Test
        fun `좋아요한 게임 → 가중치에 1_3 곱셈`() {
            val gameA = gameWithId(1)
            val gameB = gameWithId(2)
            val mgA = memberGame(gameA, StatusEnum.PLAYING, false, 0.0, null)
            val mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mgA, mgB))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(0.0f, 1.0f)),
            )
            // gameA(id=1)만 좋아요
            whenever(gameLikeRepository.findGameIdsByMemberId(MEMBER_ID)).thenReturn(listOf(1L))

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            val wA = 1.8 * 1.3 // PLAYING × liked
            val wB = 1.8
            val total = wA + wB
            assertThat(saved[0]).isCloseTo((wA / total).toFloat(), within(0.001f))
            assertThat(saved[1]).isCloseTo((wB / total).toFloat(), within(0.001f))
        }

        @Test
        fun `리뷰 평점 → 가중치에 (rating ÷ 5_0) 곱셈`() {
            val gameA = gameWithId(1)
            val gameB = gameWithId(2)
            val review = Review("Good", "Great game", 4.0, null, gameA)
            val mgA = memberGame(gameA, StatusEnum.PLAYING, false, 0.0, review)
            val mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mgA, mgB))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(0.0f, 1.0f)),
            )
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            val wA = 1.8 * (4.0 / 5.0) // PLAYING × review rating
            val wB = 1.8
            val total = wA + wB
            assertThat(saved[0]).isCloseTo((wA / total).toFloat(), within(0.001f))
            assertThat(saved[1]).isCloseTo((wB / total).toFloat(), within(0.001f))
        }

        @Test
        fun `리뷰 평점 0 → 평점 가중치 적용하지 않는다`() {
            val gameA = gameWithId(1)
            val review = Review("Meh", "No score", 0.0, null, gameA)
            val mg = memberGame(gameA, StatusEnum.PLAYING, false, 0.0, review)

            stubLibrary(listOf(mg))
            stubFeatureVectors(arrayOf(1, vectorString(1.0f, 0.0f)))
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            assertThat(saved[0]).isEqualTo(1.0f)
            assertThat(saved[1]).isEqualTo(0.0f)
        }

        @Test
        fun `모든 가중치 복합 적용 - 즐겨찾기 + COMPLETED + 플레이타임 + 좋아요 + 리뷰 vs 기본`() {
            val playtime = 50.0
            val rating = 4.5

            val gameA = gameWithId(1)
            val review = Review("Masterpiece", "Best ever", rating, null, gameA)
            val mgA = memberGame(gameA, StatusEnum.COMPLETED, true, playtime, review)

            val gameB = gameWithId(2)
            val mgB = memberGame(gameB, StatusEnum.PLAN_TO_PLAY, false, 0.0, null)

            stubLibrary(listOf(mgA, mgB))
            stubFeatureVectors(
                arrayOf(1, vectorString(1.0f, 0.0f)),
                arrayOf(2, vectorString(0.0f, 1.0f)),
            )
            // gameA만 좋아요
            whenever(gameLikeRepository.findGameIdsByMemberId(MEMBER_ID)).thenReturn(listOf(1L))

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()

            val wA = 2.5 * 2.0 * (1.0 + Math.log1p(playtime) * 0.3) * 1.3 * (rating / 5.0)
            val wB = 0.8
            val total = wA + wB

            assertThat(saved[0]).isCloseTo((wA / total).toFloat(), within(0.001f))
            assertThat(saved[1]).isCloseTo((wB / total).toFloat(), within(0.001f))
            assertThat(saved[0]).isGreaterThan(0.9f)
            assertThat(saved[1]).isLessThan(0.1f)
        }
    }

    @Nested
    inner class `벡터 크기 검증` {
        @Test
        fun `저장되는 벡터는 항상 TOTAL_DIMENSIONS 차원이다`() {
            val game = gameWithId(1)
            val mg = memberGame(game, StatusEnum.PLAYING, false, 0.0, null)

            stubLibrary(listOf(mg))
            stubFeatureVectors(arrayOf(1, fullDimensionVectorString()))
            stubLikedGameIds()

            service.calculateAndSaveProfileVector(MEMBER_ID)

            val saved = captureProfileVector()
            assertThat(saved).hasSize(TOTAL_DIMENSIONS)
        }
    }

    // --- stub 헬퍼 ---

    private fun stubLibrary(memberGames: List<MemberGame>) {
        whenever(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(memberGames)
    }

    private fun stubFeatureVectors(vararg rows: Array<out Any?>) {
        whenever(gameVectorRepository.findFeatureVectorsByMemberId(MEMBER_ID))
            .thenReturn(listOf(*rows) as List<Array<Any?>>?)
    }

    private fun stubLikedGameIds(vararg gameIds: Long) {
        whenever(gameLikeRepository.findGameIdsByMemberId(MEMBER_ID))
            .thenReturn(gameIds.toList())
    }

    private fun captureProfileVector(): FloatArray {
        val captor = argumentCaptor<String>()
        verify(memberVectorRepository).updateProfileVector(eq(MEMBER_ID), captor.capture())
        return parseVector(captor.firstValue)
    }

    // --- 팩토리 헬퍼 ---

    private fun gameWithId(id: Int): Game = GameTestFixtures.gameWithId(id.toLong(), id * 100L, "Game$id", "")

    private fun memberGame(
        game: Game,
        status: StatusEnum,
        favorite: Boolean,
        playtime: Double,
        review: Review?,
    ): MemberGame {
        val mg = MemberGame(1L, playtime, favorite, status, null, game)
        if (review != null) {
            mg.setReview(review)
        }
        return mg
    }

    private fun vectorString(
        first: Float,
        second: Float,
    ): String {
        val v = FloatArray(TOTAL_DIMENSIONS)
        v[0] = first
        v[1] = second
        return GameVectorService.vectorToString(v)
    }

    private fun fullDimensionVectorString(): String {
        val v = FloatArray(TOTAL_DIMENSIONS)
        v[0] = 1.0f
        v[TOTAL_DIMENSIONS - 1] = 0.5f
        return GameVectorService.vectorToString(v)
    }

    private fun parseVector(vectorStr: String): FloatArray {
        val inner = vectorStr.substring(1, vectorStr.length - 1)
        val parts = inner.split(",")
        return FloatArray(parts.size) { i -> parts[i].toFloat() }
    }
}
