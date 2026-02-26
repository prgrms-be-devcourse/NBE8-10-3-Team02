package com.back.global.batch

import com.back.domain.game.GameTestFixtures
import com.back.domain.game.game.entity.*
import com.back.domain.game.game.repository.*
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.service.GameVectorService
import com.back.global.batch.dto.GameBatchItem
import com.back.global.batch.writer.IgdbGameUpsertWriter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.batch.item.Chunk

@ExtendWith(MockitoExtension::class)
class IgdbGameUpsertWriterTest {
    @Mock private lateinit var gameRepository: GameRepository

    @Mock private lateinit var gameGenreRepository: GameGenreRepository

    @Mock private lateinit var gamePlatformRepository: GamePlatformRepository

    @Mock private lateinit var gameThemeRepository: GameThemeRepository

    @Mock private lateinit var gameKeywordRepository: GameKeywordRepository

    @Mock private lateinit var gameGameModeRepository: GameGameModeRepository

    @Mock private lateinit var gamePlayerPerspectiveRepository: GamePlayerPerspectiveRepository

    @Mock private lateinit var gameCompanyRepository: GameCompanyRepository

    @Mock private lateinit var gameExternalIdRepository: GameExternalIdRepository

    @Mock private lateinit var gameVectorRepository: GameVectorRepository

    @Mock private lateinit var gameVectorService: GameVectorService

    private lateinit var writer: IgdbGameUpsertWriter

    @BeforeEach
    fun setUp() {
        writer =
            IgdbGameUpsertWriter(
                gameRepository,
                gameGenreRepository,
                gamePlatformRepository,
                gameThemeRepository,
                gameKeywordRepository,
                gameGameModeRepository,
                gamePlayerPerspectiveRepository,
                gameCompanyRepository,
                gameExternalIdRepository,
                gameVectorRepository,
                gameVectorService,
            )
    }

    private fun emptyItem(game: Game) =
        GameBatchItem(
            game = game,
            genres = emptyList(),
            platforms = emptyList(),
            themes = emptyList(),
            keywords = emptyList(),
            gameModes = emptyList(),
            playerPerspectives = emptyList(),
            companies = emptyList(),
            externalIds = emptyList(),
        )

    @Test
    fun `DB에 없는 게임은 신규 저장한다`() {
        // 신규 게임이기 때문에 id = null이 맞음
        val incoming = Game.createGame(1L, "New Game", "summary", "cover1", 1700000000L, null, null, null, null)
        whenever(gameRepository.findByIgdbIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(gameRepository.save(incoming)).thenReturn(incoming)

        writer.write(Chunk(listOf(emptyItem(incoming))))

        verify(gameRepository).save(incoming)
        // 신규만 있으므로 벌크 삭제 호출 없음
        verify(gameGenreRepository, never()).deleteByGameIdIn(any())
    }

    @Test
    fun `DB에 있는 게임은 업데이트하고 벌크삭제후 재저장한다`() {
        val existing = GameTestFixtures.gameWithId(100L, 1L, "Old Name", "old summary")
        val incoming = Game.createGame(1L, "New Name", "new summary", "newCover", 1700000000L, null, null, null, null)
        val item =
            GameBatchItem(
                game = incoming,
                genres = listOf(Genre.createGenre(200L, "Action")),
                platforms = emptyList(),
                themes = emptyList(),
                keywords = emptyList(),
                gameModes = emptyList(),
                playerPerspectives = emptyList(),
                companies = emptyList(),
                externalIds = emptyList(),
            )
        whenever(gameRepository.findByIgdbIdIn(listOf(1L))).thenReturn(listOf(existing))

        writer.write(Chunk(listOf(item)))

        // 게임 자체는 dirty checking으로 업데이트 (save 호출 없음)
        verify(gameRepository, never()).save(any())
        assertThat(existing.name).isEqualTo("New Name")

        // 벌크 삭제 1회 호출
        verify(gameGenreRepository).deleteByGameIdIn(any())
        verify(gamePlatformRepository).deleteByGameIdIn(any())

        // 벌크 저장 호출
        verify(gameGenreRepository).saveAll(any<List<GameGenre>>())
    }

    /**
     * 이 테스트가 검증하려는 것
     *   1. 신규(incoming2)는 save 호출 → captor로 정확히 검증
     *   2. 기존(existing)은 dirty checking으로 업데이트 → assertThat(existing.name)으로 정확히 검증
     *   3. 벌크 삭제는 신규 게임에 대해선 호출되지 않고, 기존 게임에 대해서만 1번 호출됐다 는 것만 확인
     */
    @Test
    fun `여러 게임을 한 chunk에서 신규와 업데이트를 구분한다`() {
        val existing = GameTestFixtures.gameWithId(100L, 1L, "Existing", "summary")
        val incoming1 = Game.createGame(1L, "Updated", "new summary", "cover", 1700000000L, null, null, null, null)
        val incoming2 = Game.createGame(2L, "Brand New", "summary", "cover", 1700000000L, null, null, null, null)

        whenever(gameRepository.findByIgdbIdIn(listOf(1L, 2L))).thenReturn(listOf(existing))
        whenever(gameRepository.save(incoming2)).thenReturn(incoming2)

        writer.write(Chunk(listOf(emptyItem(incoming1), emptyItem(incoming2))))

        // incoming2만 save (신규)
        val captor = argumentCaptor<Game>()
        verify(gameRepository, times(1)).save(captor.capture())
        assertThat(captor.firstValue.igdbId).isEqualTo(2L)

        // existing은 dirty checking으로 업데이트
        assertThat(existing.name).isEqualTo("Updated")

        // 벌크 삭제는 기존 게임에 대해서만
        verify(gameGenreRepository).deleteByGameIdIn(any())
    }
}
