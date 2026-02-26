package com.back.global.batch.tasklet

import com.back.domain.game.game.entity.GameMode
import com.back.domain.game.game.repository.GameModeRepository
import com.back.global.igdb.BatchIgdbClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class GameModeSyncTasklet(
    private val igdbClient: BatchIgdbClient,
    private val gameModeRepository: GameModeRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val igdbGameModes = igdbClient.fetchGameModes()
        log.info("IGDB에서 게임 모드 {}건 조회", igdbGameModes.size)

        val igdbIds = igdbGameModes.map { it.id }
        val existingMap = gameModeRepository.findByIgdbIdIn(igdbIds).associateBy { it.igdbId }

        var created = 0
        for (dto in igdbGameModes) {
            if (!existingMap.containsKey(dto.id)) {
                gameModeRepository.save(GameMode.createGameMode(dto.id, dto.name ?: ""))
                created++
            }
        }

        log.info("게임 모드 동기화 완료: 신규 {}건, 기존 {}건 스킵", created, igdbGameModes.size - created)
        return RepeatStatus.FINISHED
    }
}
