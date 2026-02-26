package com.back.global.batch

import com.back.domain.game.game.entity.Company
import com.back.domain.game.game.entity.CompanyRole
import com.back.domain.game.game.entity.Genre
import com.back.domain.game.game.entity.Platform
import com.back.domain.game.game.repository.CompanyRepository
import com.back.domain.game.game.repository.GameModeRepository
import com.back.domain.game.game.repository.GenreRepository
import com.back.domain.game.game.repository.KeywordRepository
import com.back.domain.game.game.repository.PlatformRepository
import com.back.domain.game.game.repository.PlayerPerspectiveRepository
import com.back.domain.game.game.repository.ThemeRepository
import com.back.global.batch.processor.IgdbGameProcessor
import com.back.global.igdb.dto.IgdbCompanyDto
import com.back.global.igdb.dto.IgdbGameDetailDto
import com.back.global.igdb.dto.IgdbGenreDto
import com.back.global.igdb.dto.IgdbInvolvedCompanyDto
import com.back.support.IgdbFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class IgdbGameProcessorTest {
    @Mock private lateinit var genreRepository: GenreRepository

    @Mock private lateinit var platformRepository: PlatformRepository

    @Mock private lateinit var themeRepository: ThemeRepository

    @Mock private lateinit var gameModeRepository: GameModeRepository

    @Mock private lateinit var playerPerspectiveRepository: PlayerPerspectiveRepository

    @Mock private lateinit var keywordRepository: KeywordRepository

    @Mock private lateinit var companyRepository: CompanyRepository

    private lateinit var processor: IgdbGameProcessor

    @BeforeEach
    fun setUp() {
        // whenever는 이 메서드가 호출되면 이 값을 반환해라 라는 스텁(stub) 설정
        // findAll이 호출되면 listOf(Action, RPG) 반환
        whenever(genreRepository.findAll()).thenReturn(
            listOf(
                Genre.createGenre(10000L, "Action"),
                Genre.createGenre(10001L, "RPG"),
            ),
        )
        whenever(platformRepository.findAll()).thenReturn(
            listOf(
                Platform.createPlatform(10001L, "PC (Windows)"),
                Platform.createPlatform(20001L, "PlayStation 5"),
            ),
        )
        whenever(themeRepository.findAll()).thenReturn(emptyList())
        whenever(gameModeRepository.findAll()).thenReturn(emptyList())
        whenever(playerPerspectiveRepository.findAll()).thenReturn(emptyList())
        whenever(keywordRepository.findAll()).thenReturn(emptyList())
        whenever(companyRepository.findAll()).thenReturn(
            listOf(
                Company.createCompany(10000L, "testCompany"),
                Company.createCompany(1L, "DevCo"),
                Company.createCompany(2L, "PubCo"),
            ),
        )

        processor =
            IgdbGameProcessor(
                genreRepository,
                platformRepository,
                themeRepository,
                gameModeRepository,
                playerPerspectiveRepository,
                keywordRepository,
                companyRepository,
            )
    }

    @Test
    fun `정상 DTO를 GameBatchItem으로 변환한다`() {
        val item = processor.process(IgdbFixtures.gameDetail(1L))

        assertThat(item).isNotNull()
        assertThat(item!!.game.igdbId).isEqualTo(1L)
        assertThat(item.game.name).isEqualTo("Test Game 1")
        assertThat(item.game.summary).isEqualTo("summary-1")
        assertThat(item.game.coverImageId).isEqualTo("coverImage")
        assertThat(item.companies).hasSize(2) // testCompany as DEVELOPER + PUBLISHER
        assertThat(item.genres).hasSize(2)
        assertThat(item.platforms).hasSize(2)
    }

    @Test
    fun `name이 null이면 null을 반환한다`() {
        val dto = minimalDto(name = null)

        assertThat(processor.process(dto)).isNull()
    }

    @Test
    fun `summary가 null이면 빈문자열로 설정한다`() {
        val item = processor.process(minimalDto(summary = null))

        assertThat(item!!.game.summary).isEmpty()
    }

    @Test
    fun `summary가 5000자 초과해도 그대로 유지한다`() {
        val item = processor.process(minimalDto(summary = "A".repeat(6000)))

        assertThat(item!!.game.summary).hasSize(6000)
    }

    @Test
    fun `cover가 null이면 coverImageId가 null이다`() {
        val item = processor.process(minimalDto())

        assertThat(item!!.game.coverImageId).isNull()
    }

    @Test
    fun `캐시에 없는 장르는 무시한다`() {
        val dto = minimalDto(genres = listOf(IgdbGenreDto(99999L, "Unknown")))

        assertThat(processor.process(dto)!!.genres).isEmpty()
    }

    @Test
    fun `involvedCompanies에서 developer와 publisher를 분리한다`() {
        val dto =
            minimalDto(
                involvedCompanies =
                    listOf(
                        IgdbInvolvedCompanyDto(1L, IgdbCompanyDto(1L, "DevCo"), developer = true, publisher = false),
                        IgdbInvolvedCompanyDto(2L, IgdbCompanyDto(2L, "PubCo"), developer = false, publisher = true),
                    ),
            )

        val item = processor.process(dto)!!

        assertThat(item.companies).hasSize(2)
        assertThat(item.companies).anyMatch { it.company.name == "DevCo" && it.role == CompanyRole.DEVELOPER }
        assertThat(item.companies).anyMatch { it.company.name == "PubCo" && it.role == CompanyRole.PUBLISHER }
    }

    private fun minimalDto(
        name: String? = "Game",
        summary: String? = "summary",
        genres: List<IgdbGenreDto>? = null,
        involvedCompanies: List<IgdbInvolvedCompanyDto>? = null,
    ) = IgdbGameDetailDto(
        id = 1L,
        name = name,
        summary = summary,
        firstReleaseDateEpochSeconds = 1700000000L,
        cover = null,
        involvedCompanies = involvedCompanies,
        genres = genres,
        platforms = null,
        storyline = null,
        themes = null,
        keywords = null,
        gameModes = null,
        playerPerspectives = null,
        externalGames = null,
        franchises = null,
        aggregatedRating = null,
    )
}
