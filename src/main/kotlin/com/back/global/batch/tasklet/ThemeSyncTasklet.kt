package com.back.global.batch.tasklet

import com.back.domain.game.game.entity.Theme
import com.back.domain.game.game.repository.ThemeRepository
import com.back.global.igdb.BatchIgdbClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class ThemeSyncTasklet(
    private val igdbClient: BatchIgdbClient,
    private val themeRepository: ThemeRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val igdbThemes = igdbClient.fetchThemes()
        log.info("IGDB에서 테마 {}건 조회", igdbThemes.size)

        val igdbIds = igdbThemes.map { it.id }
        val existingMap = themeRepository.findByIgdbIdIn(igdbIds).associateBy { it.igdbId }

        var created = 0
        for (dto in igdbThemes) {
            if (!existingMap.containsKey(dto.id)) {
                themeRepository.save(Theme.createTheme(dto.id, dto.name ?: ""))
                created++
            }
        }

        log.info("테마 동기화 완료: 신규 {}건, 기존 {}건 스킵", created, igdbThemes.size - created)
        return RepeatStatus.FINISHED
    }
}
