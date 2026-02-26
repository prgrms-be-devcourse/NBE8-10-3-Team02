package com.back.global.batch.tasklet

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class DropVectorIndexTasklet(
    private val entityManager: EntityManager,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        log.info("[Batch] Dropping HNSW index: {}", VectorBatchSql.VECTOR_INDEX_NAME)
        entityManager.createNativeQuery(VectorBatchSql.DROP_VECTOR_INDEX).executeUpdate()
        log.info("[Batch] Dropped HNSW index: {}", VectorBatchSql.VECTOR_INDEX_NAME)
        return RepeatStatus.FINISHED
    }
}
