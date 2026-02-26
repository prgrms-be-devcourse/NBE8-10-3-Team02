package com.back.domain.game.recommendation.service

import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.gameLike.entity.GameLike
import com.back.domain.game.gameLike.repository.GameLikeRepository
import com.back.domain.game.recommendation.dto.GameRecommendationResponse
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.repository.MemberVectorRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.entity.MemberGame
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.global.vector.VectorDimensionMapper
import com.back.global.vector.VectorDimensionMapper.TOTAL_DIMENSIONS
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class GameRecommendationIntegrationTest {
    @PersistenceContext
    private lateinit var em: EntityManager

    @Autowired private lateinit var gameRepository: GameRepository

    @Autowired private lateinit var gameVectorRepository: GameVectorRepository

    @Autowired private lateinit var memberVectorRepository: MemberVectorRepository

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var memberGameRepository: MemberGameRepository

    @Autowired private lateinit var gameLikeRepository: GameLikeRepository

    @Autowired private lateinit var recommendationService: GameRecommendationService

    @Autowired private lateinit var userProfileVectorService: UserProfileVectorService

    private lateinit var member: Member

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(Member("test@test.com", "password", "tester"))
    }

    @Nested
    inner class `코사인 유사도 기반 유사 게임 조회` {
        @Test
        fun `같은 장르 벡터를 가진 게임이 다른 장르보다 유사도가 높다`() {
            // given: RPG 게임(target), RPG 게임(같은 장르), FPS 게임(다른 장르)
            saveGameWithVector(1L, "RPG Target", rpgVector())
            saveGameWithVector(2L, "RPG Similar", rpgVector())
            saveGameWithVector(3L, "FPS Game", fpsVector())

            // when
            val results = recommendationService.getSimilarGames(1L, 10)

            // then: RPG Similar가 FPS Game보다 유사도 높음
            assertThat(results).hasSizeGreaterThanOrEqualTo(2)
            val rpgResult = findByName(results, "RPG Similar")
            val fpsResult = findByName(results, "FPS Game")
            assertThat(rpgResult.similarity).isGreaterThan(fpsResult.similarity)
        }

        @Test
        fun `자기 자신은 유사 게임 결과에 포함되지 않는다`() {
            saveGameWithVector(10L, "Target", rpgVector())
            saveGameWithVector(11L, "Other", rpgVector())

            val results = recommendationService.getSimilarGames(10L, 10)

            assertThat(results)
                .extracting<String?> { it.name }
                .doesNotContain("Target")
                .contains("Other")
        }

        @Test
        fun `feature_vector가 NULL인 게임은 결과에 포함되지 않는다`() {
            saveGameWithVector(20L, "WithVector", rpgVector())
            saveGameWithVector(21L, "Target", rpgVector())
            // NULL 벡터 게임 (벡터 업데이트 안 함)
            gameRepository.save(Game.createGame(22L, "NoVector", "summary", null, 1700000000L, null, null, null, null))

            val results = recommendationService.getSimilarGames(21L, 10)

            assertThat(results)
                .extracting<String?> { it.name }
                .contains("WithVector")
                .doesNotContain("NoVector")
        }

        @Test
        fun `동일 벡터의 코사인 유사도는 1 0이다`() {
            saveGameWithVector(30L, "Target", rpgVector())
            saveGameWithVector(31L, "Identical", rpgVector())

            val results = recommendationService.getSimilarGames(30L, 10)

            val result = findByName(results, "Identical")
            assertThat(result.similarity).isCloseTo(1.0, Offset.offset(0.01))
        }

        @Test
        fun `직교 벡터(겹치는 장르 없음)의 코사인 유사도는 0 0이다`() {
            saveGameWithVector(40L, "Target", rpgVector())
            saveGameWithVector(41L, "Orthogonal", fpsVector())

            val results = recommendationService.getSimilarGames(40L, 10)

            val result = findByName(results, "Orthogonal")
            assertThat(result.similarity).isCloseTo(0.0, Offset.offset(0.01))
        }
    }

    @Nested
    inner class `개인 맞춤 추천` {
        @Test
        fun `RPG 프로필 유저에게 RPG 게임이 FPS 게임보다 상위 추천된다`() {
            // given: 유저가 RPG 게임을 보유 → 프로필 벡터 = RPG
            val ownedRpg = saveGameWithVector(50L, "Owned RPG", rpgVector())
            memberGameRepository.save(MemberGame(1L, 100.0, true, StatusEnum.COMPLETED, member, ownedRpg))

            // 추천 후보: RPG 3개 + FPS 3개
            saveGameWithVector(51L, "RPG Candidate 1", rpgVector())
            saveGameWithVector(52L, "RPG Candidate 2", rpgVector())
            saveGameWithVector(53L, "RPG Candidate 3", rpgVector())
            saveGameWithVector(54L, "FPS Candidate 1", fpsVector())
            saveGameWithVector(55L, "FPS Candidate 2", fpsVector())
            saveGameWithVector(56L, "FPS Candidate 3", fpsVector())

            // 프로필 벡터 생성
            userProfileVectorService.calculateAndSaveProfileVector(member.id)

            // when
            val results = recommendationService.getPersonalRecommendations(member.id, 6)

            // then: 상위 3개가 RPG
            assertThat(results).hasSize(6)
            val topThreeNames = results.subList(0, 3).map { it.name }
            assertThat(topThreeNames).allMatch { it?.startsWith("RPG") == true }
        }

        @Test
        fun `보유 게임은 추천 결과에서 제외된다`() {
            val owned = saveGameWithVector(60L, "Owned", rpgVector())
            saveGameWithVector(61L, "Candidate", rpgVector())
            memberGameRepository.save(MemberGame(1L, 0.0, false, StatusEnum.PLAYING, member, owned))

            userProfileVectorService.calculateAndSaveProfileVector(member.id)

            val results = recommendationService.getPersonalRecommendations(member.id, 10)

            assertThat(results)
                .extracting<String?> { it.name }
                .doesNotContain("Owned")
                .contains("Candidate")
        }

        @Test
        fun `하이브리드 스코어가 높은 게임이 상위에 온다 (similarity + rating + likeCount)`() {
            // 유저 프로필: RPG
            val owned = saveGameWithVector(70L, "Owned", rpgVector())
            memberGameRepository.save(MemberGame(1L, 0.0, false, StatusEnum.PLAYING, member, owned))

            // 두 RPG 게임: 유사도 동일하지만 rating/likeCount 다름
            saveGameWithVector(71L, "HighRated RPG", rpgVector(), 95.0, 1000)
            saveGameWithVector(72L, "LowRated RPG", rpgVector(), 10.0, 0)

            userProfileVectorService.calculateAndSaveProfileVector(member.id)

            val results = recommendationService.getPersonalRecommendations(member.id, 2)

            assertThat(results[0].name).isEqualTo("HighRated RPG")
            assertThat(results[0].score).isGreaterThan(results[1].score)
        }
    }

    @Nested
    inner class `프로필 벡터 + 가중치 통합 검증` {
        @Test
        fun `즐겨찾기 + COMPLETED 게임이 프로필 벡터에 더 강하게 반영된다`() {
            // RPG 게임: 즐겨찾기 + COMPLETED (높은 가중치)
            // FPS 게임: PLAN_TO_PLAY (낮은 가중치)
            val rpg = saveGameWithVector(80L, "Fav RPG", rpgVector())
            val fps = saveGameWithVector(81L, "Plan FPS", fpsVector())
            memberGameRepository.save(MemberGame(1L, 200.0, true, StatusEnum.COMPLETED, member, rpg))
            memberGameRepository.save(MemberGame(1L, 0.0, false, StatusEnum.PLAN_TO_PLAY, member, fps))

            // 추천 후보
            saveGameWithVector(82L, "RPG Candidate", rpgVector())
            saveGameWithVector(83L, "FPS Candidate", fpsVector())

            userProfileVectorService.calculateAndSaveProfileVector(member.id)

            val results = recommendationService.getPersonalRecommendations(member.id, 2)

            // RPG 가중치가 훨씬 높으므로 RPG가 1위
            assertThat(results[0].name).isEqualTo("RPG Candidate")
        }

        @Test
        fun `좋아요한 게임이 프로필 벡터에 1 3배 반영된다`() {
            // 두 게임 모두 PLAYING, 동일 조건이지만 하나만 좋아요
            val rpg = saveGameWithVector(90L, "Liked RPG", rpgVector())
            val fps = saveGameWithVector(91L, "Unliked FPS", fpsVector())
            memberGameRepository.save(MemberGame(1L, 0.0, false, StatusEnum.PLAYING, member, rpg))
            memberGameRepository.save(MemberGame(1L, 0.0, false, StatusEnum.PLAYING, member, fps))
            // RPG에만 좋아요
            gameLikeRepository.save(GameLike.createGameLike(member, rpg))

            saveGameWithVector(92L, "RPG Candidate", rpgVector())
            saveGameWithVector(93L, "FPS Candidate", fpsVector())

            userProfileVectorService.calculateAndSaveProfileVector(member.id)

            val results = recommendationService.getPersonalRecommendations(member.id, 2)

            // 좋아요 가중치(1.3×)로 RPG 방향이 더 강함
            assertThat(results[0].name).isEqualTo("RPG Candidate")
        }
    }

    @Nested
    inner class `벌크 벡터 업데이트` {
        @Test
        fun `bulkUpdateFeatureVectors로 저장한 벡터가 유사도 검색에 사용된다`() {
            val game1 = gameRepository.save(Game.createGame(100L, "Bulk1", "s", null, 1700000000L, null, null, null, null))
            val game2 = gameRepository.save(Game.createGame(101L, "Bulk2", "s", null, 1700000000L, null, null, null, null))

            em.flush()
            // staging에 UPSERT 후 game 테이블에 반영
            gameVectorRepository.bulkUpdateFeatureVectors(
                mapOf(
                    game1.id!! to vectorToString(rpgVector()),
                    game2.id!! to vectorToString(fpsVector()),
                ),
            )
            gameVectorRepository.applyStagingToGameAndTruncate()
            em.clear()

            // rpgVector 기준 검색 → Bulk1(RPG)이 Bulk2(FPS)보다 유사도 높음
            saveGameWithVector(102L, "SearchTarget", rpgVector())
            val results = recommendationService.getSimilarGames(102L, 10)

            val bulk1 = findByName(results, "Bulk1")
            val bulk2 = findByName(results, "Bulk2")
            assertThat(bulk1.similarity).isGreaterThan(bulk2.similarity)
        }
    }

    // --- 벡터 헬퍼 ---

    /** RPG 벡터: Genre index 0 = 1.0 (나머지 0) */
    private fun rpgVector(): FloatArray {
        val v = FloatArray(TOTAL_DIMENSIONS)
        v[VectorDimensionMapper.GENRE_OFFSET] = 1.0f
        return v
    }

    /** FPS 벡터: Genre index 1 = 1.0 (나머지 0) — RPG와 직교 */
    private fun fpsVector(): FloatArray {
        val v = FloatArray(TOTAL_DIMENSIONS)
        v[VectorDimensionMapper.GENRE_OFFSET + 1] = 1.0f
        return v
    }

    private fun saveGameWithVector(
        igdbId: Long,
        name: String,
        vector: FloatArray,
        rating: Double = 0.0,
        likeCount: Long = 0,
    ): Game {
        var game = Game.createGame(igdbId, name, "summary", null, 1700000000L, null, rating, null, null)
        game = gameRepository.save(game)

        // staging 테이블에 UPSERT 후 game 테이블에 반영
        gameVectorRepository.bulkUpdateFeatureVectors(mapOf(game.id!! to vectorToString(vector)))
        gameVectorRepository.applyStagingToGameAndTruncate()
        em.clear()
        return game
    }

    private fun vectorToString(vector: FloatArray): String = GameVectorService.vectorToString(vector)

    private fun findByName(
        results: List<GameRecommendationResponse>,
        name: String,
    ): GameRecommendationResponse =
        results.firstOrNull { it.name == name }
            ?: throw AssertionError("결과에 '$name' 게임이 없습니다. 실제 결과: ${results.map { it.name }}")
}
