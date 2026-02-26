package com.back.global.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
@Profile("dev", "prod")
class BatchTriggerService(
    private val jobLauncher: JobLauncher,
    private val igdbSyncJob: Job,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun runIgdbSyncAsync() {
        try {
            val params =
                JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters()

            log.info("비동기 IGDB 동기화 Job 시작")
            val execution = jobLauncher.run(igdbSyncJob, params)
            log.info("IGDB 동기화 Job 완료: status={}", execution.exitStatus.exitCode)
        } catch (e: Exception) {
            log.error("IGDB 동기화 Job 실행 실패", e)
        }
    }
}
