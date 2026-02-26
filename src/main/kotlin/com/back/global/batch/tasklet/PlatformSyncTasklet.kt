package com.back.global.batch.tasklet

import com.back.domain.game.game.entity.Platform
import com.back.domain.game.game.repository.PlatformRepository
import com.back.global.igdb.BatchIgdbClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class PlatformSyncTasklet(
    private val igdbClient: BatchIgdbClient,
    private val platformRepository: PlatformRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val igdbPlatforms = igdbClient.fetchPlatforms()
        log.info("IGDB에서 플랫폼 {}건 조회", igdbPlatforms.size)

        val igdbIds = igdbPlatforms.map { it.id }
        val existingMap = platformRepository.findByIgdbIdIn(igdbIds).associateBy { it.igdbId }

        var created = 0
        for (dto in igdbPlatforms) {
            if (!existingMap.containsKey(dto.id)) {
                platformRepository.save(Platform.createPlatform(dto.id, dto.name ?: ""))
                created++
            }
        }

        log.info("플랫폼 동기화 완료: 신규 {}건, 기존 {}건 스킵", created, igdbPlatforms.size - created)
        return RepeatStatus.FINISHED
    }
}
