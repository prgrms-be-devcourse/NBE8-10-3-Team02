package com.back.global.batch.tasklet

import com.back.domain.game.game.entity.Company
import com.back.domain.game.game.repository.CompanyRepository
import com.back.global.igdb.BatchIgdbClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class CompanySyncTasklet(
    private val igdbClient: BatchIgdbClient,
    private val companyRepository: CompanyRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val igdbCompanies = igdbClient.fetchCompanies()
        log.info("IGDB에서 회사 {}건 조회", igdbCompanies.size)

        val existingMap = companyRepository.findAll().associateBy { it.igdbId }

        val toSave =
            igdbCompanies
                .filter { !existingMap.containsKey(it.id) }
                .map { Company.createCompany(it.id, it.name ?: "") }

        if (toSave.isNotEmpty()) {
            companyRepository.saveAll(toSave)
        }

        log.info("회사 동기화 완료: 신규 {}건, 기존 {}건 스킵", toSave.size, igdbCompanies.size - toSave.size)
        return RepeatStatus.FINISHED
    }
}
