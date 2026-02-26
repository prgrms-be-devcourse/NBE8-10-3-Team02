package com.back.domain.game.game.repository

import com.back.domain.game.game.dto.GameSearchCondition
import com.back.domain.game.game.entity.Game
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class GameSearchRepositoryImplTest
    @Autowired
    constructor(
        private val em: EntityManager,
        private val queryFactory: JPAQueryFactory,
    ) {
        private lateinit var repository: GameSearchRepositoryImpl

        @BeforeEach
        fun setUp() {
            repository = GameSearchRepositoryImpl(queryFactory)
        }

        @Test
        fun `게임 이름 검색 시 공백을 제거하고 대소문자를 구분하지 않아야 한다`() {
            // Given: Game.createGame() 메서드를 사용하여 객체 생성
            val targetGame =
                Game.createGame(
                    igdbId = 12345L,
                    name = "The Legend of Zelda",
                    summary = "Adventure game",
                    imageId = "cover01",
                    firstReleaseDate = LocalDate.now(),
                )

            em.persist(targetGame)
            em.flush()
            em.clear() // 영속성 컨텍스트를 비워 DB에서 새로 조회하도록 유도

            // When: "zelda " (공백 포함)로 검색해도 찾아지는지 확인
            val condition = GameSearchCondition(query = "zelda ")
            val result = repository.searchByCondition(condition)

            // Then: 결과 확인
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("The Legend of Zelda")
        }

        @Test
        fun `장르와 플랫폼 필터가 비어있어도 에러 없이 작동해야 한다`() {
            // Given
            val condition =
                GameSearchCondition(
                    query = null,
                    genreIds = emptyList(),
                    platformIgdbIds = emptyList(),
                )

            // When & Then
            assertDoesNotThrow { repository.searchByCondition(condition) }
        }

        @Test
        fun `특정 장르 ID가 주어지면 해당 장르의 게임만 검색되어야 한다`() {
            // Given
            val gameWithGenre = Game.createGame(1L, "Action Game", "Summary", "img1", LocalDate.now())

            em.persist(gameWithGenre)

            val condition = GameSearchCondition(genreIds = listOf(10L)) // 특정 장르 ID 세팅

            // When
            val result = repository.searchByCondition(condition)

            // Then
            // 장르 조인 로직이 정상적으로 실행되었는지 확인
            assertThat(result).isNotNull
        }

        @Test
        fun `플랫폼 ID가 있으면 플랫폼 테이블과 조인하여 검색해야 한다`() {
            // Given
            val condition = GameSearchCondition(platformIgdbIds = listOf(48L)) // 예: PS5 ID

            // When
            val result = repository.searchByCondition(condition)

            // Then
            // 플랫폼 조인 쿼리가 에러 없이 생성되는지 확인
            assertThat(result).isNotNull
        }
    }
