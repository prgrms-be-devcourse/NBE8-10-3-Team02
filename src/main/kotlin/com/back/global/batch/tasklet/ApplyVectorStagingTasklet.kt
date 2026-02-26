package com.back.global.batch.tasklet

import com.back.domain.game.recommendation.repository.GameVectorRepository
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class ApplyVectorStagingTasklet(
    private val gameVectorRepository: GameVectorRepository,
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        gameVectorRepository.applyStagingToGameAndTruncate()
        return RepeatStatus.FINISHED
    }
}
