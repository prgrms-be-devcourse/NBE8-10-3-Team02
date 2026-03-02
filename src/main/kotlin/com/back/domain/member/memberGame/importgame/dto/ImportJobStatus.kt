package com.back.domain.member.memberGame.importgame.dto

data class ImportJobStatus(
    val jobId: String,
    val status: String,
    val processed: Int,
    val total: Int,
    val result: ImportMatchResponse?,
) {
    companion object {
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"

        fun processing(
            jobId: String,
            processed: Int,
            total: Int,
        ) = ImportJobStatus(jobId, STATUS_PROCESSING, processed, total, null)

        fun completed(
            jobId: String,
            total: Int,
            result: ImportMatchResponse,
        ) = ImportJobStatus(jobId, STATUS_COMPLETED, total, total, result)

        fun failed(
            jobId: String,
            processed: Int,
            total: Int,
        ) = ImportJobStatus(jobId, STATUS_FAILED, processed, total, null)
    }
}
