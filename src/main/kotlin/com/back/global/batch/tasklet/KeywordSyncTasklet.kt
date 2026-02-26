package com.back.global.batch.tasklet

import com.back.domain.game.game.entity.Keyword
import com.back.domain.game.game.repository.KeywordRepository
import com.back.global.igdb.BatchIgdbClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class KeywordSyncTasklet(
    private val igdbClient: BatchIgdbClient,
    private val keywordRepository: KeywordRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val igdbKeywords = igdbClient.fetchKeywords()
        log.info("IGDB에서 키워드 {}건 조회", igdbKeywords.size)

        val existingMap = keywordRepository.findAll().associateBy { it.igdbId }

        val toSave =
            igdbKeywords
                .filter { !existingMap.containsKey(it.id) }
                .map { Keyword.createKeyword(it.id, it.name ?: "") }

        if (toSave.isNotEmpty()) {
            keywordRepository.saveAll(toSave)
        }

        log.info("키워드 동기화 완료: 신규 {}건, 기존 {}건 스킵", toSave.size, igdbKeywords.size - toSave.size)
        return RepeatStatus.FINISHED
    }
}
