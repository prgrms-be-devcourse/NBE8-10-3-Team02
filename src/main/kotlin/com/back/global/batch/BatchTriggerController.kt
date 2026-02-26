package com.back.global.batch

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("dev", "prod")
class BatchTriggerController(
    private val batchTriggerService: BatchTriggerService,
) {
    @PostMapping("/batch/igdb-sync")
    fun triggerIgdbSync(): Map<String, String> {
        batchTriggerService.runIgdbSyncAsync()
        return mapOf("status" to "STARTED", "message" to "Job이 백그라운드에서 실행 중입니다.")
    }
}
