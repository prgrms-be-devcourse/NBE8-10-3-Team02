package com.back.global.batch.writer

import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.entity.GameCompany
import com.back.domain.game.game.entity.GameExternalId
import com.back.domain.game.game.entity.GameGameMode
import com.back.domain.game.game.entity.GameGenre
import com.back.domain.game.game.entity.GameKeyword
import com.back.domain.game.game.entity.GamePlatform
import com.back.domain.game.game.entity.GamePlayerPerspective
import com.back.domain.game.game.entity.GameTheme
import com.back.domain.game.game.repository.GameCompanyRepository
import com.back.domain.game.game.repository.GameExternalIdRepository
import com.back.domain.game.game.repository.GameGameModeRepository
import com.back.domain.game.game.repository.GameGenreRepository
import com.back.domain.game.game.repository.GameKeywordRepository
import com.back.domain.game.game.repository.GamePlatformRepository
import com.back.domain.game.game.repository.GamePlayerPerspectiveRepository
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.game.repository.GameThemeRepository
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.service.GameVectorService
import com.back.global.batch.dto.GameBatchItem
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import java.time.ZoneOffset

class IgdbGameUpsertWriter(
    private val gameRepository: GameRepository,
    private val gameGenreRepository: GameGenreRepository,
    private val gamePlatformRepository: GamePlatformRepository,
    private val gameThemeRepository: GameThemeRepository,
    private val gameKeywordRepository: GameKeywordRepository,
    private val gameGameModeRepository: GameGameModeRepository,
    private val gamePlayerPerspectiveRepository: GamePlayerPerspectiveRepository,
    private val gameCompanyRepository: GameCompanyRepository,
    private val gameExternalIdRepository: GameExternalIdRepository,
    private val gameVectorRepository: GameVectorRepository,
    private val gameVectorService: GameVectorService,
) : ItemWriter<GameBatchItem> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun write(chunk: Chunk<out GameBatchItem>) {
        // 1. 기존 게임 조회
        val igdbIds = chunk.items.map { it.game.igdbId }
        val existingMap = gameRepository.findByIgdbIdIn(igdbIds).associateBy { it.igdbId }

        // 2. 신규/업데이트 분리 + 게임 persist
        val persistedMap = mutableMapOf<Long, Game>()
        val existingGameIds = mutableListOf<Long>()
        var created = 0
        var updated = 0

        for (item in chunk) {
            val incoming = item.game
            val existing = existingMap[incoming.igdbId]

            if (existing == null) {
                val saved = gameRepository.save(incoming)
                persistedMap[incoming.igdbId] = saved
                created++
            } else {
                existing.updateDetail(
                    incoming.name,
                    incoming.summary,
                    incoming.coverImageId,
                    incoming.firstReleaseDate?.atStartOfDay()?.toEpochSecond(ZoneOffset.UTC),
                    incoming.storyline,
                    incoming.aggregatedRating,
                    incoming.franchiseIgdbId,
                    incoming.franchiseName,
                )
                existingGameIds.add(existing.id!!)
                persistedMap[incoming.igdbId] = existing
                updated++
            }
        }

        // 3. 기존 게임의 join 엔티티 벌크 삭제 (8쿼리)
        if (existingGameIds.isNotEmpty()) {
            gameGenreRepository.deleteByGameIdIn(existingGameIds)
            gamePlatformRepository.deleteByGameIdIn(existingGameIds)
            gameThemeRepository.deleteByGameIdIn(existingGameIds)
            gameKeywordRepository.deleteByGameIdIn(existingGameIds)
            gameGameModeRepository.deleteByGameIdIn(existingGameIds)
            gamePlayerPerspectiveRepository.deleteByGameIdIn(existingGameIds)
            gameCompanyRepository.deleteByGameIdIn(existingGameIds)
            gameExternalIdRepository.deleteByGameIdIn(existingGameIds)
        }

        // 4. 모든 게임(신규+기존)의 join 엔티티 수집
        val allGenres = mutableListOf<GameGenre>()
        val allPlatforms = mutableListOf<GamePlatform>()
        val allThemes = mutableListOf<GameTheme>()
        val allKeywords = mutableListOf<GameKeyword>()
        val allGameModes = mutableListOf<GameGameMode>()
        val allPlayerPerspectives = mutableListOf<GamePlayerPerspective>()
        val allCompanies = mutableListOf<GameCompany>()
        val allExternalIds = mutableListOf<GameExternalId>()

        for (item in chunk) {
            val game = persistedMap[item.game.igdbId] ?: continue
            item.genres.forEach { allGenres.add(GameGenre.createGameGenre(game, it)) }
            item.platforms.forEach { allPlatforms.add(GamePlatform.createGamePlatform(game, it)) }
            item.themes.forEach { allThemes.add(GameTheme.createGameTheme(game, it)) }
            item.keywords.forEach { allKeywords.add(GameKeyword.createGameKeyword(game, it)) }
            item.gameModes.forEach { allGameModes.add(GameGameMode.createGameGameMode(game, it)) }
            item.playerPerspectives.forEach {
                allPlayerPerspectives.add(GamePlayerPerspective.createGamePlayerPerspective(game, it))
            }
            item.companies.forEach { allCompanies.add(GameCompany.createGameCompany(game, it.company, it.role)) }
            item.externalIds.forEach {
                allExternalIds.add(GameExternalId.createGameExternalId(game, it.platform, it.externalId))
            }
        }

        // 5. 벌크 저장 (8쿼리)
        if (allGenres.isNotEmpty()) gameGenreRepository.saveAll(allGenres)
        if (allPlatforms.isNotEmpty()) gamePlatformRepository.saveAll(allPlatforms)
        if (allThemes.isNotEmpty()) gameThemeRepository.saveAll(allThemes)
        if (allKeywords.isNotEmpty()) gameKeywordRepository.saveAll(allKeywords)
        if (allGameModes.isNotEmpty()) gameGameModeRepository.saveAll(allGameModes)
        if (allPlayerPerspectives.isNotEmpty()) gamePlayerPerspectiveRepository.saveAll(allPlayerPerspectives)
        if (allCompanies.isNotEmpty()) gameCompanyRepository.saveAll(allCompanies)
        if (allExternalIds.isNotEmpty()) gameExternalIdRepository.saveAll(allExternalIds)

        // 6. 게임 피처 벡터 벌크 생성 (메모리에 있는 속성 데이터로 바로 생성)
        val vectorMap = mutableMapOf<Long, String>()
        for (item in chunk) {
            val game = persistedMap[item.game.igdbId] ?: continue
            try {
                val vector =
                    gameVectorService.buildFeatureVector(
                        item.genres,
                        item.themes,
                        item.keywords,
                        item.gameModes,
                        item.playerPerspectives,
                    )
                vectorMap[game.id!!] = GameVectorService.vectorToString(vector)
            } catch (e: Exception) {
                log.warn("게임 벡터 생성 실패 gameId={}: {}", game.id, e.message)
            }
        }
        if (vectorMap.isNotEmpty()) {
            gameVectorRepository.bulkUpdateFeatureVectors(vectorMap)
        }

        log.debug("게임 upsert: 신규 {}건, 갱신 {}건, 벡터 {}건", created, updated, vectorMap.size)
    }
}
