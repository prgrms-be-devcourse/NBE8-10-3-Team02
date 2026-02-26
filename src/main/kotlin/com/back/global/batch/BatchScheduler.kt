package com.back.global.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BatchScheduler(
    private val jobLauncher: JobLauncher,
    private val igdbSyncJob: Job,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * MON")
    fun runIgdbSyncJob() {
        try {
            val params =
                JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters()

            log.info("IGDB 동기화 Job 시작")
            jobLauncher.run(igdbSyncJob, params)
            log.info("IGDB 동기화 Job 완료")
        } catch (e: Exception) {
            log.error("IGDB 동기화 Job 실행 실패", e)
        }
    }
}
