package com.back.global.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("batch")
class BatchJobRunner(
    private val jobLauncher: JobLauncher,
    private val igdbSyncJob: Job,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val params = JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()

        try {
            log.info("배치 Job 실행 시작")
            jobLauncher.run(igdbSyncJob, params)
            log.info("배치 Job 실행 완료 → 프로세스 종료")
            exitProcess(0)
        } catch (e: Exception) {
            log.error("배치 Job 실행 실패 → 프로세스 종료", e)
            exitProcess(1)
        }
    }
}
