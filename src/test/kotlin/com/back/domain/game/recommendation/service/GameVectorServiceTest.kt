package com.back.domain.game.recommendation.service

import com.back.domain.game.game.entity.GameMode
import com.back.domain.game.game.entity.Genre
import com.back.domain.game.game.entity.Keyword
import com.back.domain.game.game.entity.PlayerPerspective
import com.back.domain.game.game.entity.Theme
import com.back.global.vector.VectorDimensionMapper
import com.back.global.vector.VectorDimensionMapper.GENRE_OFFSET
import com.back.global.vector.VectorDimensionMapper.KEYWORD_OFFSET
import com.back.global.vector.VectorDimensionMapper.MODE_OFFSET
import com.back.global.vector.VectorDimensionMapper.PERSPECTIVE_OFFSET
import com.back.global.vector.VectorDimensionMapper.THEME_OFFSET
import com.back.global.vector.VectorDimensionMapper.TOTAL_DIMENSIONS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GameVectorServiceTest {
    private lateinit var mapper: VectorDimensionMapper
    private lateinit var service: GameVectorService

    @BeforeEach
    fun setUp() {
        mapper = VectorDimensionMapper()
        mapper.refresh(
            mapOf(1L to 0, 2L to 1, 3L to 5), // genre: id→index
            mapOf(10L to 0, 11L to 3), // theme
            mapOf(100L to 0, 101L to 50, 102L to 99), // keyword
            mapOf(200L to 0, 201L to 2), // gameMode
            mapOf(300L to 0, 301L to 4), // perspective
        )
        service = GameVectorService(mapper)
    }

    @Nested
    inner class `buildFeatureVector` {
        @Test
        fun `모든 속성이 비어있으면 전부 0_0인 벡터를 반환한다`() {
            val vector =
                service.buildFeatureVector(
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(),
                )

            assertThat(vector).hasSize(TOTAL_DIMENSIONS)
            assertThat(vector).containsOnly(0.0f)
        }

        @Test
        fun `장르 2개 → 해당 오프셋+인덱스 위치만 1_0`() {
            val vector =
                service.buildFeatureVector(
                    listOf(genre(1L), genre(2L)),
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(),
                )

            assertThat(vector).hasSize(TOTAL_DIMENSIONS)
            assertThat(vector[GENRE_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(vector[GENRE_OFFSET + 1]).isEqualTo(1.0f)
            assertThat(vector[GENRE_OFFSET + 2]).isEqualTo(0.0f)
            assertThat(countOnes(vector)).isEqualTo(2)
        }

        @Test
        fun `테마 → THEME_OFFSET 기준으로 올바른 위치에 1_0`() {
            val vector =
                service.buildFeatureVector(
                    listOf(),
                    listOf(theme(10L), theme(11L)),
                    listOf(),
                    listOf(),
                    listOf(),
                )

            assertThat(vector[THEME_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(vector[THEME_OFFSET + 3]).isEqualTo(1.0f)
            assertThat(vector[THEME_OFFSET + 1]).isEqualTo(0.0f)
            assertThat(countOnes(vector)).isEqualTo(2)
        }

        @Test
        fun `키워드 → KEYWORD_OFFSET 기준으로 올바른 위치에 1_0`() {
            val vector =
                service.buildFeatureVector(
                    listOf(),
                    listOf(),
                    listOf(keyword(100L), keyword(102L)),
                    listOf(),
                    listOf(),
                )

            assertThat(vector[KEYWORD_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(vector[KEYWORD_OFFSET + 99]).isEqualTo(1.0f)
            assertThat(vector[KEYWORD_OFFSET + 50]).isEqualTo(0.0f)
            assertThat(countOnes(vector)).isEqualTo(2)
        }

        @Test
        fun `게임모드 → MODE_OFFSET 기준으로 올바른 위치에 1_0`() {
            val vector =
                service.buildFeatureVector(
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(gameMode(201L)),
                    listOf(),
                )

            assertThat(vector[MODE_OFFSET + 2]).isEqualTo(1.0f)
            assertThat(vector[MODE_OFFSET + 0]).isEqualTo(0.0f)
        }

        @Test
        fun `플레이어 시점 → PERSPECTIVE_OFFSET 기준으로 올바른 위치에 1_0`() {
            val vector =
                service.buildFeatureVector(
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(perspective(301L)),
                )

            assertThat(vector[PERSPECTIVE_OFFSET + 4]).isEqualTo(1.0f)
            assertThat(vector[PERSPECTIVE_OFFSET + 0]).isEqualTo(0.0f)
        }

        @Test
        fun `5종 속성을 모두 넣으면 각 구간에 올바르게 1_0이 설정된다`() {
            val vector =
                service.buildFeatureVector(
                    listOf(genre(1L)),
                    listOf(theme(10L)),
                    listOf(keyword(101L)),
                    listOf(gameMode(200L)),
                    listOf(perspective(300L)),
                )

            assertThat(vector).hasSize(TOTAL_DIMENSIONS)
            assertThat(vector[GENRE_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(vector[THEME_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(vector[KEYWORD_OFFSET + 50]).isEqualTo(1.0f)
            assertThat(vector[MODE_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(vector[PERSPECTIVE_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(countOnes(vector)).isEqualTo(5)
        }

        @Test
        fun `매핑에 없는 id는 무시하고 벡터에 반영하지 않는다`() {
            val vector =
                service.buildFeatureVector(
                    listOf(genre(999L)),
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(),
                )

            assertThat(vector).hasSize(TOTAL_DIMENSIONS)
            assertThat(vector).containsOnly(0.0f)
        }

        @Test
        fun `매핑에 있는 id와 없는 id가 섞여있으면 있는 것만 반영된다`() {
            val vector =
                service.buildFeatureVector(
                    listOf(genre(1L), genre(999L)),
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(),
                )

            assertThat(vector[GENRE_OFFSET + 0]).isEqualTo(1.0f)
            assertThat(countOnes(vector)).isEqualTo(1)
        }

        @Test
        fun `매퍼가 refresh 되지 않은 빈 상태면 전부 0_0`() {
            val emptyMapper = VectorDimensionMapper()
            val emptyService = GameVectorService(emptyMapper)

            val vector =
                emptyService.buildFeatureVector(
                    listOf(genre(1L)),
                    listOf(theme(10L)),
                    listOf(keyword(100L)),
                    listOf(gameMode(200L)),
                    listOf(perspective(300L)),
                )

            assertThat(vector).hasSize(TOTAL_DIMENSIONS)
            assertThat(vector).containsOnly(0.0f)
        }

        @Test
        fun `같은 속성 구간 내에서 인덱스가 겹치지 않는다`() {
            val vector =
                service.buildFeatureVector(
                    listOf(genre(3L)), // index 5
                    listOf(theme(11L)), // index 3
                    listOf(keyword(102L)), // index 99
                    listOf(gameMode(201L)), // index 2
                    listOf(perspective(301L)), // index 4
                )

            assertThat(vector[GENRE_OFFSET + 5]).isEqualTo(1.0f)
            assertThat(vector[THEME_OFFSET + 3]).isEqualTo(1.0f)
            assertThat(vector[KEYWORD_OFFSET + 99]).isEqualTo(1.0f)
            assertThat(vector[MODE_OFFSET + 2]).isEqualTo(1.0f)
            assertThat(vector[PERSPECTIVE_OFFSET + 4]).isEqualTo(1.0f)

            // Genre 구간의 index 3 위치는 0.0이어야 함 (Theme의 index 3과 혼동 없음)
            assertThat(vector[GENRE_OFFSET + 3]).isEqualTo(0.0f)
        }
    }

    @Nested
    inner class `vectorToString` {
        @Test
        fun `pgvector 포맷 v0,v1, 형식으로 변환된다`() {
            val vector = floatArrayOf(1.0f, 0.0f, 0.5f)

            val result = GameVectorService.vectorToString(vector)

            assertThat(result).isEqualTo("[1.0,0.0,0.5]")
        }

        @Test
        fun `빈 배열은 빈 대괄호로 변환된다`() {
            val result = GameVectorService.vectorToString(floatArrayOf())

            assertThat(result).isEqualTo("[]")
        }

        @Test
        fun `TOTAL_DIMENSIONS 크기 벡터도 올바르게 변환된다`() {
            val vector = FloatArray(TOTAL_DIMENSIONS)
            vector[0] = 1.0f
            vector[TOTAL_DIMENSIONS - 1] = 1.0f

            val result = GameVectorService.vectorToString(vector)

            assertThat(result).startsWith("[1.0,")
            assertThat(result).endsWith(",1.0]")
            val commaCount = result.count { it == ',' }.toLong()
            assertThat(commaCount).isEqualTo((TOTAL_DIMENSIONS - 1).toLong())
        }
    }

    // --- vector에서 1.0f count 헬퍼 ---
    private fun countOnes(vector: FloatArray): Long = vector.count { it == 1.0f }.toLong()

    // --- 테스트용 엔티티 팩토리 ---

    private fun genre(id: Long): Genre {
        val g = Genre(id * 10, "Genre$id")
        setEntityId(g, id)
        return g
    }

    private fun theme(id: Long): Theme {
        val t = Theme(id * 10, "Theme$id")
        setEntityId(t, id)
        return t
    }

    private fun keyword(id: Long): Keyword {
        val k = Keyword(id * 10, "Keyword$id")
        setEntityId(k, id)
        return k
    }

    private fun gameMode(id: Long): GameMode {
        val gm = GameMode(id * 10, "GameMode$id")
        setEntityId(gm, id)
        return gm
    }

    private fun perspective(id: Long): PlayerPerspective {
        val pp = PlayerPerspective(id * 10, "Perspective$id")
        setEntityId(pp, id)
        return pp
    }

    private fun setEntityId(
        target: Any,
        id: Long,
    ) {
        target.javaClass.getDeclaredField("id").also {
            it.isAccessible = true
            it.set(target, id)
        }
    }
}
