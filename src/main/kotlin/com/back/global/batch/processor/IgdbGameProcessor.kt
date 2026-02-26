package com.back.global.batch.processor

import com.back.domain.game.game.entity.Company
import com.back.domain.game.game.entity.CompanyRole
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.entity.GameMode
import com.back.domain.game.game.entity.Genre
import com.back.domain.game.game.entity.Keyword
import com.back.domain.game.game.entity.Platform
import com.back.domain.game.game.entity.PlayerPerspective
import com.back.domain.game.game.entity.Theme
import com.back.domain.game.game.repository.CompanyRepository
import com.back.domain.game.game.repository.GameModeRepository
import com.back.domain.game.game.repository.GenreRepository
import com.back.domain.game.game.repository.KeywordRepository
import com.back.domain.game.game.repository.PlatformRepository
import com.back.domain.game.game.repository.PlayerPerspectiveRepository
import com.back.domain.game.game.repository.ThemeRepository
import com.back.global.batch.dto.GameBatchItem
import com.back.global.igdb.dto.IgdbGameDetailDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import java.util.concurrent.ConcurrentHashMap

open class IgdbGameProcessor(
    genreRepository: GenreRepository,
    platformRepository: PlatformRepository,
    themeRepository: ThemeRepository,
    gameModeRepository: GameModeRepository,
    playerPerspectiveRepository: PlayerPerspectiveRepository,
    keywordRepository: KeywordRepository,
    companyRepository: CompanyRepository,
) : ItemProcessor<IgdbGameDetailDto, GameBatchItem> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val genreCache = ConcurrentHashMap<Long, Genre>()
    private val platformCache = ConcurrentHashMap<Long, Platform>()
    private val themeCache = ConcurrentHashMap<Long, Theme>()
    private val gameModeCache = ConcurrentHashMap<Long, GameMode>()
    private val playerPerspectiveCache = ConcurrentHashMap<Long, PlayerPerspective>()
    private val keywordCache = ConcurrentHashMap<Long, Keyword>()
    private val companyCache = ConcurrentHashMap<Long, Company>()

    init {
        genreRepository.findAll().forEach { genreCache[it.igdbId] = it }
        platformRepository.findAll().forEach { platformCache[it.igdbId] = it }
        themeRepository.findAll().forEach { themeCache[it.igdbId] = it }
        gameModeRepository.findAll().forEach { gameModeCache[it.igdbId] = it }
        playerPerspectiveRepository.findAll().forEach { playerPerspectiveCache[it.igdbId] = it }
        keywordRepository.findAll().forEach { keywordCache[it.igdbId] = it }
        companyRepository.findAll().forEach { companyCache[it.igdbId] = it }

        log.info(
            "프로세서 캐시 초기화: 장르 {}건, 플랫폼 {}건, 테마 {}건, 게임모드 {}건, 시점 {}건, 키워드 {}건, 회사 {}건",
            genreCache.size,
            platformCache.size,
            themeCache.size,
            gameModeCache.size,
            playerPerspectiveCache.size,
            keywordCache.size,
            companyCache.size,
        )
    }

    override fun process(dto: IgdbGameDetailDto): GameBatchItem? {
        if (dto.name == null) return null

        val summary = dto.summary ?: ""
        val coverImageId = dto.cover?.imageId
        val firstFranchise = dto.franchises?.firstOrNull()
        val franchiseIgdbId = firstFranchise?.id
        val franchiseName = firstFranchise?.name

        val game =
            Game.createGame(
                dto.id,
                dto.name,
                summary,
                coverImageId,
                dto.firstReleaseDateEpochSeconds,
                dto.storyline,
                dto.aggregatedRating,
                franchiseIgdbId,
                franchiseName,
            )

        val genres = dto.genres?.mapNotNull { genreCache[it.id] } ?: emptyList()
        val platforms = dto.platforms?.mapNotNull { platformCache[it.id] } ?: emptyList()
        val themes = dto.themes?.mapNotNull { themeCache[it.id] } ?: emptyList()
        val keywords = dto.keywords?.mapNotNull { keywordCache[it.id] } ?: emptyList()
        val gameModes = dto.gameModes?.mapNotNull { gameModeCache[it.id] } ?: emptyList()
        val playerPerspectives = dto.playerPerspectives?.mapNotNull { playerPerspectiveCache[it.id] } ?: emptyList()

        // Involved Companies (deduplicate by company + role)
        val companies = mutableListOf<GameBatchItem.CompanyRoleEntry>()
        if (dto.involvedCompanies != null) {
            val developers = mutableSetOf<Long>()
            val publishers = mutableSetOf<Long>()
            for (ic in dto.involvedCompanies) {
                val companyDto = ic.company ?: continue
                val company = companyCache[companyDto.id] ?: continue
                if (ic.developer == true && developers.add(company.igdbId)) {
                    companies.add(GameBatchItem.CompanyRoleEntry(company, CompanyRole.DEVELOPER))
                }
                if (ic.publisher == true && publishers.add(company.igdbId)) {
                    companies.add(GameBatchItem.CompanyRoleEntry(company, CompanyRole.PUBLISHER))
                }
            }
        }

        // External Games (Steam only: externalGameSource == 1)
        // IGDB가 동일한 uid를 가진 external_game 항목을 중복으로 반환할 수 있어 distinctBy로 제거
        val externalIds =
            dto.externalGames
                ?.filter { it.externalGameSource == 1L && it.uid != null }
                ?.map { GameBatchItem.ExternalIdEntry("STEAM", it.uid!!) }
                ?.distinctBy { Pair(it.platform, it.externalId) }
                ?: emptyList()

        return GameBatchItem(
            game = game,
            genres = genres,
            platforms = platforms,
            themes = themes,
            keywords = keywords,
            gameModes = gameModes,
            playerPerspectives = playerPerspectives,
            companies = companies,
            externalIds = externalIds,
        )
    }
}
