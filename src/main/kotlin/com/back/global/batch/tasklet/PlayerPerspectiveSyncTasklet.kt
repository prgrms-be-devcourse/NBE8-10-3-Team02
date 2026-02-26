package com.back.global.batch.tasklet

import com.back.domain.game.game.entity.PlayerPerspective
import com.back.domain.game.game.repository.PlayerPerspectiveRepository
import com.back.global.igdb.BatchIgdbClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class PlayerPerspectiveSyncTasklet(
    private val igdbClient: BatchIgdbClient,
    private val playerPerspectiveRepository: PlayerPerspectiveRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val igdbPlayerPerspectives = igdbClient.fetchPlayerPerspectives()
        log.info("IGDB에서 플레이어 시점 {}건 조회", igdbPlayerPerspectives.size)

        val igdbIds = igdbPlayerPerspectives.map { it.id }
        val existingMap = playerPerspectiveRepository.findByIgdbIdIn(igdbIds).associateBy { it.igdbId }

        var created = 0
        for (dto in igdbPlayerPerspectives) {
            if (!existingMap.containsKey(dto.id)) {
                playerPerspectiveRepository.save(PlayerPerspective.createPlayerPerspective(dto.id, dto.name ?: ""))
                created++
            }
        }

        log.info("플레이어 시점 동기화 완료: 신규 {}건, 기존 {}건 스킵", created, igdbPlayerPerspectives.size - created)
        return RepeatStatus.FINISHED
    }
}
