package com.back.domain.game.game.controller

import com.back.domain.game.game.dto.GameDetailResponse
import com.back.domain.game.game.dto.GameVideoResponse
import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.service.GameService
import com.back.global.exception.ServiceException
import com.back.global.igdb.IgdbClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1GameControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var gameService: GameService

    @MockitoBean
    private lateinit var igdbClient: IgdbClient

    @Test
    fun `게임상세조회 - 성공`() {
        val igdbId = 10L
        val game = Game.createGame(igdbId, "Zelda", "summary", "co", null, null, null, null, null)
        Game::class.java.getDeclaredField("id").also {
            it.isAccessible = true
            it.set(game, 1L)
        }
        whenever(gameService.getGameDetail(igdbId))
            .thenReturn(
                GameDetailResponse.from(
                    game,
                    listOf("Action"),
                    listOf("Switch"),
                    listOf("Nintendo"),
                    listOf("Nintendo"),
                ),
            )

        mockMvc
            .get("/api/v1/games/{igdbId}", igdbId)
            .andExpect {
                status { isOk() }
                jsonPath("$.gameId") { value(1) }
                jsonPath("$.igdbId") { value(10) }
                jsonPath("$.gameName") { value("Zelda") }
                jsonPath("$.coverImageId") { value("co") }
                jsonPath("$.developers[0]") { value("Nintendo") }
                jsonPath("$.publishers[0]") { value("Nintendo") }
                jsonPath("$.genres[0]") { value("Action") }
                jsonPath("$.platforms[0]") { value("Switch") }
            }
    }

    @Test
    fun `게임상세조회 - 없는 게임`() {
        whenever(gameService.getGameDetail(999L))
            .thenThrow(ServiceException("404-1", "게임을 찾을 수 없습니다. 999"))

        mockMvc
            .get("/api/v1/games/{igdbId}", 999L)
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `비디오 조회 - 성공`() {
        whenever(gameService.getVideoId(10L))
            .thenReturn(GameVideoResponse.from("abc123"))

        mockMvc
            .get("/api/v1/games/{igdbId}/video", 10L)
            .andExpect {
                status { isOk() }
                jsonPath("$.videoId") { value("abc123") }
            }
    }

    @Test
    fun `비슷한 게임 조회 - 성공`() {
        whenever(gameService.getSimilarGames(10L))
            .thenReturn(listOf(SimilarGameResponse(20L, "Similar Game", "cover123")))

        mockMvc
            .get("/api/v1/games/{igdbId}/similarGames", 10L)
            .andExpect {
                status { isOk() }
                jsonPath("$[0].name") { value("Similar Game") }
            }
    }
}
