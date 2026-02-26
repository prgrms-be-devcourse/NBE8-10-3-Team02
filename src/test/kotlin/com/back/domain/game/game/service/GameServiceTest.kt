package com.back.domain.game.game.service

import com.back.domain.game.game.dto.GameDetailResponse
import com.back.domain.game.game.dto.SimilarGameResponse
import com.back.domain.game.game.entity.Company
import com.back.domain.game.game.entity.CompanyRole
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.entity.GameCompany
import com.back.domain.game.game.entity.GameGenre
import com.back.domain.game.game.entity.GamePlatform
import com.back.domain.game.game.entity.Genre
import com.back.domain.game.game.entity.Platform
import com.back.domain.game.game.repository.CompanyRepository
import com.back.domain.game.game.repository.GameCompanyRepository
import com.back.domain.game.game.repository.GameGenreRepository
import com.back.domain.game.game.repository.GamePlatformRepository
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.game.repository.GenreRepository
import com.back.domain.game.game.repository.PlatformRepository
import com.back.domain.game.recommendation.dto.GameRecommendationResponse
import com.back.domain.game.recommendation.service.GameRecommendationService
import com.back.global.exception.ServiceException
import com.back.global.igdb.IgdbClient
import com.back.global.igdb.dto.IgdbVideoDto
import com.github.benmanes.caffeine.cache.Cache
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class GameServiceTest {
    @Autowired private lateinit var gameService: GameService

    @Autowired private lateinit var gameRepository: GameRepository

    @MockitoBean private lateinit var igdbClient: IgdbClient

    @MockitoBean private lateinit var gameRecommendationService: GameRecommendationService

    @Autowired private lateinit var genreRepository: GenreRepository

    @Autowired private lateinit var platformRepository: PlatformRepository

    @Autowired private lateinit var gameGenreRepository: GameGenreRepository

    @Autowired private lateinit var gamePlatformRepository: GamePlatformRepository

    @Autowired private lateinit var gameCompanyRepository: GameCompanyRepository

    @Autowired private lateinit var companyRepository: CompanyRepository

    @Autowired private lateinit var gameDetailCache: Cache<Long, GameDetailResponse>

    @Autowired private lateinit var similarListCache: Cache<Long, List<SimilarGameResponse>>

    @BeforeEach
    fun setUp() {
        gameDetailCache.invalidateAll()
        similarListCache.invalidateAll()
    }

    @Test
    fun `게임상세조회 - DB에서 조회 성공`() {
        val igdbId = 999L
        val game = Game.createGame(igdbId, "Test Game 999", "summary-999", "coverImage", 1700000000L, null, null, null, null)
        gameRepository.save(game)

        val result = gameService.getGameDetail(igdbId)

        assertThat(result).isNotNull()
        assertThat(result.gameName).isEqualTo("Test Game 999")
        verify(igdbClient, never()).getGameDetail(any())
    }

    @Test
    fun `게임상세조회 - 두 번째 호출은 캐시에서 반환`() {
        val igdbId = 888L
        val game = Game.createGame(igdbId, "Test Game 888", "summary-888", "coverImage", 1700000000L, null, null, null, null)
        gameRepository.save(game)

        gameService.getGameDetail(igdbId) // 1차: DB 조회
        val result = gameService.getGameDetail(igdbId) // 2차: 캐시

        assertThat(result.gameName).isEqualTo("Test Game 888")
    }

    @Test
    fun `비디오 조회 - API 호출`() {
        val igdbId = 777L
        whenever(igdbClient.getVideoId(igdbId)).thenReturn(IgdbVideoDto(1L, "video123"))

        val result = gameService.getVideoId(igdbId)

        assertThat(result.videoId).isEqualTo("video123")
        verify(igdbClient, times(1)).getVideoId(igdbId)
    }

    @Test
    fun `비디오 조회 - 두 번째 호출은 캐시에서 반환`() {
        val igdbId = 666L
        whenever(igdbClient.getVideoId(igdbId)).thenReturn(IgdbVideoDto(1L, "cachedVideo"))

        gameService.getVideoId(igdbId) // 1차
        val result = gameService.getVideoId(igdbId) // 2차

        assertThat(result.videoId).isEqualTo("cachedVideo")
        verify(igdbClient, times(1)).getVideoId(igdbId)
    }

    @Test
    fun `비슷한 게임 조회 - pgvector 추천 호출`() {
        val igdbId = 555L
        whenever(gameRecommendationService.getSimilarGames(igdbId, 10)).thenReturn(
            listOf(
                GameRecommendationResponse(1, "Similar1", "co1", 0.9, 0.85),
                GameRecommendationResponse(2, "Similar2", "co2", 0.8, 0.75),
            ),
        )

        val result = gameService.getSimilarGames(igdbId)

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("Similar1")
        verify(gameRecommendationService, times(1)).getSimilarGames(igdbId, 10)
    }

    @Test
    fun `비슷한 게임 조회 - 두 번째 호출은 캐시에서 반환`() {
        val igdbId = 444L
        whenever(gameRecommendationService.getSimilarGames(igdbId, 10)).thenReturn(
            listOf(
                GameRecommendationResponse(1, "Cached", "co", 0.9, 0.85),
            ),
        )

        gameService.getSimilarGames(igdbId) // 1차
        val result = gameService.getSimilarGames(igdbId) // 2차

        assertThat(result).hasSize(1)
        verify(gameRecommendationService, times(1)).getSimilarGames(igdbId, 10)
    }

    @Test
    fun `비디오 없으면 - 빈 문자열 반환`() {
        val igdbId = 333L
        whenever(igdbClient.getVideoId(igdbId)).thenReturn(null)

        val result = gameService.getVideoId(igdbId)

        assertThat(result.videoId).isEmpty()
    }

    @Test
    fun `비슷한 게임 없으면 - 빈 리스트 반환`() {
        val igdbId = 222L
        whenever(gameRecommendationService.getSimilarGames(igdbId, 10)).thenReturn(listOf())

        val result = gameService.getSimilarGames(igdbId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `게임상세조회 - 캐시 miss, DB hit 일때 API호출 안 함`() {
        val igdbId = 1009L
        val existingGame = Game.createGame(igdbId, "Existing Game", "Already in DB", "existingCover", 1700000000L, null, null, null, null)
        gameRepository.save(existingGame)
        gameDetailCache.invalidate(igdbId)

        val result = gameService.getGameDetail(igdbId)

        assertThat(result).isNotNull()
        assertThat(result.gameName).isEqualTo("Existing Game")
        verify(igdbClient, never()).getGameDetail(igdbId)
    }

    @Test
    fun `게임상세조회 - DB에 없으면 예외 발생`() {
        val igdbId = 1011L

        assertThatThrownBy { gameService.getGameDetail(igdbId) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("게임을 찾을 수 없습니다. $igdbId")
    }

    @Test
    fun `비디오 조회 - dto는 있지만 videoId가 null이면 빈 문자열 반환`() {
        val igdbId = 1012L
        whenever(igdbClient.getVideoId(igdbId)).thenReturn(IgdbVideoDto(1001L, null))

        val result = gameService.getVideoId(igdbId)

        assertThat(result.videoId).isEmpty()
    }

    @Test
    fun `게임상세조회 - Genre가 정상적으로 반환됨`() {
        val igdbId = 1013L
        val game = Game.createGame(igdbId, "Genre Game", "summary", "cover", 1700000000L, null, null, null, null)
        gameRepository.save(game)

        val action = genreRepository.save(Genre.createGenre(10000L, "Action"))
        val rpg = genreRepository.save(Genre.createGenre(10001L, "RPG"))
        gameGenreRepository.save(GameGenre.createGameGenre(game, action))
        gameGenreRepository.save(GameGenre.createGameGenre(game, rpg))

        val result = gameService.getGameDetail(igdbId)

        assertThat(result.genres).containsExactlyInAnyOrder("Action", "RPG")
    }

    @Test
    fun `게임상세조회 - Platform이 정상적으로 반환됨`() {
        val igdbId = 1014L
        val game = Game.createGame(igdbId, "Platform Game", "summary", "cover", 1700000000L, null, null, null, null)
        gameRepository.save(game)

        val pc = platformRepository.save(Platform.createPlatform(10001L, "PC (Windows)"))
        val ps5 = platformRepository.save(Platform.createPlatform(20001L, "PlayStation 5"))
        gamePlatformRepository.save(GamePlatform.createGamePlatform(game, pc))
        gamePlatformRepository.save(GamePlatform.createGamePlatform(game, ps5))

        val result = gameService.getGameDetail(igdbId)

        assertThat(result.platforms).containsExactlyInAnyOrder("PC (Windows)", "PlayStation 5")
    }

    @Test
    fun `게임상세조회 - Developer,Publisher가 정상적으로 반환됨`() {
        val igdbId = 1015L
        val game = Game.createGame(igdbId, "Company Game", "summary", "cover", 1700000000L, null, null, null, null)
        gameRepository.save(game)

        val company = companyRepository.save(Company.createCompany(10000L, "Nintendo"))
        gameCompanyRepository.save(GameCompany.createGameCompany(game, company, CompanyRole.DEVELOPER))
        gameCompanyRepository.save(GameCompany.createGameCompany(game, company, CompanyRole.PUBLISHER))

        val result = gameService.getGameDetail(igdbId)

        assertThat(result.developers).containsExactly("Nintendo")
        assertThat(result.publishers).containsExactly("Nintendo")
    }

    @Test
    fun `비슷한 게임 조회 - 추천 결과가 빈 리스트이면 빈 리스트 반환`() {
        val igdbId = 1016L
        whenever(gameRecommendationService.getSimilarGames(igdbId, 10)).thenReturn(listOf())

        val result = gameService.getSimilarGames(igdbId)

        assertThat(result).isEmpty()
    }
}
