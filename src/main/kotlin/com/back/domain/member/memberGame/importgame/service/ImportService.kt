package com.back.domain.member.memberGame.importgame.service

import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.game.service.GameService
import com.back.domain.member.member.service.MemberService
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.dto.MemberGameAddRequest
import com.back.domain.member.memberGame.importgame.dto.AlternativeMatch
import com.back.domain.member.memberGame.importgame.dto.ImportConfirmRequest
import com.back.domain.member.memberGame.importgame.dto.ImportConfirmResponse
import com.back.domain.member.memberGame.importgame.dto.ImportGameItem
import com.back.domain.member.memberGame.importgame.dto.ImportJobStatus
import com.back.domain.member.memberGame.importgame.dto.ImportMatchCandidate
import com.back.domain.member.memberGame.importgame.dto.ImportMatchRequest
import com.back.domain.member.memberGame.importgame.dto.ImportMatchResponse
import com.back.domain.member.memberGame.importgame.dto.ImportResultItem
import com.back.domain.member.memberGame.importgame.util.GameNameMatcher
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.domain.member.memberGame.service.MemberGameService
import com.back.global.exception.ServiceException
import com.back.global.igdb.IgdbClient
import com.back.global.igdb.dto.IgdbGameSummaryDto
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class ImportService(
    private val gameRepository: GameRepository,
    private val memberGameRepository: MemberGameRepository,
    private val memberGameService: MemberGameService,
    private val memberService: MemberService,
    private val gameService: GameService,
    private val igdbClient: IgdbClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val jobStore = ConcurrentHashMap<String, ImportJobStatus>()

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.4
    }

    fun matchGamesAsync(
        memberId: Int,
        request: ImportMatchRequest,
    ): String {
        val jobId = UUID.randomUUID().toString()
        val total = request.gameNames.size
        jobStore[jobId] = ImportJobStatus.processing(jobId, 0, total)
        processMatchingAsync(jobId, memberId, request)
        return jobId
    }

    @Suppress("TooGenericExceptionCaught")
    @Async("importTaskExecutor")
    fun processMatchingAsync(
        jobId: String,
        memberId: Int,
        request: ImportMatchRequest,
    ) {
        val gameNames = request.gameNames
        val sourcePlatform = request.sourcePlatform
        val total = gameNames.size
        val matches = mutableListOf<ImportMatchCandidate>()
        var alreadyInLibrary = 0

        try {
            for ((i, originalName) in gameNames.withIndex()) {
                val trimmed = originalName.trim()
                if (trimmed.isEmpty()) {
                    jobStore[jobId] = ImportJobStatus.processing(jobId, i + 1, total)
                    continue
                }

                val candidate = matchSingleGame(trimmed, memberId, sourcePlatform)
                if (candidate != null) {
                    matches.add(candidate)
                    if (candidate.alreadyInLibrary) alreadyInLibrary++
                } else {
                    matches.add(
                        ImportMatchCandidate(
                            originalName = trimmed,
                            igdbId = null,
                            matchedName = null,
                            coverImageId = null,
                            confidence = 0.0,
                            alreadyInLibrary = false,
                            suggestedPlatform = sourcePlatform,
                            alternatives = emptyList(),
                        ),
                    )
                }

                jobStore[jobId] = ImportJobStatus.processing(jobId, i + 1, total)

                if (i < gameNames.size - 1) {
                    Thread.sleep(250)
                }
            }

            val totalMatched = matches.count { it.igdbId != null }
            val response = ImportMatchResponse(matches, total, totalMatched, alreadyInLibrary)
            jobStore[jobId] = ImportJobStatus.completed(jobId, total, response)
        } catch (e: Exception) {
            log.error("Import matching job {} failed", jobId, e)
            jobStore[jobId] = ImportJobStatus.failed(jobId, matches.size, total)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun matchSingleGame(
        originalName: String,
        memberId: Int,
        sourcePlatform: String?,
    ): ImportMatchCandidate? {
        // 1. Local DB first
        val localMatches = gameRepository.findByNameIgnoreCaseContaining(originalName)
        if (localMatches.isNotEmpty()) {
            val bestLocal = findBestMatch(originalName, localMatches)
            if (bestLocal != null) {
                val confidence = GameNameMatcher.computeConfidence(originalName, bestLocal.name)
                if (confidence >= CONFIDENCE_THRESHOLD) {
                    val inLibrary = memberGameRepository
                        .findByMemberIdAndGameId(memberId, requireNotNull(bestLocal.id)) != null
                    val alternatives = localMatches
                        .filter { it.id != bestLocal.id }
                        .take(3)
                        .map { AlternativeMatch(requireNotNull(it.igdbId), it.name ?: "", it.coverImageId) }
                    return ImportMatchCandidate(
                        originalName = originalName,
                        igdbId = bestLocal.igdbId,
                        matchedName = bestLocal.name,
                        coverImageId = bestLocal.coverImageId,
                        confidence = confidence,
                        alreadyInLibrary = inLibrary,
                        suggestedPlatform = sourcePlatform ?: "PC",
                        alternatives = alternatives,
                    )
                }
            }
        }

        // 2. Fallback to IGDB search
        return try {
            val igdbResults = igdbClient.searchGames(originalName, 5)
            if (igdbResults.isEmpty()) return null

            val bestIgdb = findBestIgdbMatch(originalName, igdbResults) ?: return null
            val confidence = GameNameMatcher.computeConfidence(originalName, bestIgdb.name)
            if (confidence < CONFIDENCE_THRESHOLD) return null

            val existingGame = gameRepository.findByIgdbId(requireNotNull(bestIgdb.id))
            val inLibrary = existingGame
                .map { memberGameRepository.findByMemberIdAndGameId(memberId, requireNotNull(it.id)) != null }
                .orElse(false)
            val coverImageId = existingGame.map { it.coverImageId }.orElse(null)
            val alternatives = igdbResults
                .filter { it.id != bestIgdb.id }
                .take(3)
                .map { AlternativeMatch(it.id, it.name ?: "", null) }

            ImportMatchCandidate(
                originalName = originalName,
                igdbId = bestIgdb.id,
                matchedName = bestIgdb.name,
                coverImageId = coverImageId,
                confidence = confidence,
                alreadyInLibrary = inLibrary,
                suggestedPlatform = sourcePlatform ?: "PC",
                alternatives = alternatives,
            )
        } catch (e: Exception) {
            log.warn("IGDB search failed for '{}': {}", originalName, e.message)
            null
        }
    }

    private fun findBestMatch(
        originalName: String,
        candidates: List<Game>,
    ): Game? =
        candidates.maxByOrNull { GameNameMatcher.computeConfidence(originalName, it.name) }

    private fun findBestIgdbMatch(
        originalName: String,
        candidates: List<IgdbGameSummaryDto>,
    ): IgdbGameSummaryDto? =
        candidates.maxByOrNull { GameNameMatcher.computeConfidence(originalName, it.name) }

    fun getJobStatus(jobId: String): ImportJobStatus? = jobStore[jobId]

    @Suppress("TooGenericExceptionCaught")
    @Transactional
    fun confirmImport(
        memberId: Int,
        request: ImportConfirmRequest,
    ): ImportConfirmResponse {
        val member = memberService.findById(memberId)
            ?: throw ServiceException("404", "Member not found: $memberId")

        var added = 0
        var duplicates = 0
        var failed = 0
        val results = mutableListOf<ImportResultItem>()

        for (item in request.games) {
            try {
                val game = resolveGame(item)
                if (game == null) {
                    results.add(ImportResultItem(item.igdbId, null, ImportResultItem.STATUS_ERROR))
                    failed++
                    continue
                }

                if (memberGameRepository.findByMemberIdAndGameId(memberId, requireNotNull(game.id)) != null) {
                    results.add(ImportResultItem(item.igdbId, game.name, ImportResultItem.STATUS_DUPLICATE))
                    duplicates++
                    continue
                }

                val platform = item.platform ?: "PC"
                val addRequest = MemberGameAddRequest(
                    platform = platform,
                    playtime = 0.0,
                    isFavorite = false,
                    status = StatusEnum.PLAN_TO_PLAY,
                    gameId = requireNotNull(game.id),
                )
                memberGameService.addToLibrary(addRequest, member, game)
                results.add(ImportResultItem(item.igdbId, game.name, ImportResultItem.STATUS_ADDED))
                added++
            } catch (e: Exception) {
                log.warn("Failed to import game {}: {}", item.igdbId, e.message)
                results.add(ImportResultItem(item.igdbId, null, ImportResultItem.STATUS_ERROR))
                failed++
            }
        }

        return ImportConfirmResponse(added, duplicates, failed, results)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun resolveGame(item: ImportGameItem): Game? {
        val existing = gameRepository.findByIgdbId(item.igdbId)
        if (existing.isPresent) return existing.get()

        return try {
            gameService.getGameDetail(item.igdbId)
            gameRepository.findByIgdbId(item.igdbId).orElse(null)
        } catch (e: Exception) {
            log.warn("Failed to fetch game {} from IGDB: {}", item.igdbId, e.message)
            null
        }
    }
}
