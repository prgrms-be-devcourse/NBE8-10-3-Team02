package com.back.global.batch.listener

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class DiscordBatchNotifier(
    @Value("\${discord.webhook-url:}") private val webhookUrl: String,
) : JobExecutionListener {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient: RestClient = RestClient.create()

    override fun beforeJob(jobExecution: JobExecution) {
        if (!isEnabled()) return
        send(
            formatEmbed(
                "IGDB 동기화 배치 시작",
                0x3498DB,
                "시작 시간: ${formatTime(jobExecution.startTime)}",
            ),
        )
    }

    override fun afterJob(jobExecution: JobExecution) {
        if (!isEnabled()) return

        val status = jobExecution.status
        val success = status == BatchStatus.COMPLETED
        val duration = formatDuration(jobExecution.startTime, jobExecution.endTime)
        val stepExecutions = jobExecution.stepExecutions

        val description =
            buildString {
                append("**상태**: $status\n")
                append("**소요 시간**: $duration\n")
                append("**시작**: ${formatTime(jobExecution.startTime)}\n")
                append("**종료**: ${formatTime(jobExecution.endTime)}\n\n")
                append("**Step 실행 결과**\n")
                for (step in stepExecutions) {
                    val icon = if (step.status == BatchStatus.COMPLETED) "✅" else "❌"
                    append("$icon ${step.stepName} — R:${step.readCount} W:${step.writeCount} S:${step.skipCount}\n")
                }
                val totalRead = stepExecutions.sumOf { it.readCount.toLong() }
                val totalWrite = stepExecutions.sumOf { it.writeCount.toLong() }
                val totalSkip = stepExecutions.sumOf { it.skipCount.toLong() }
                append("\n**총 처리**: Read $totalRead / Write $totalWrite / Skip $totalSkip")
                if (!success) {
                    append("\n\n**에러 정보**\n")
                    for (step in stepExecutions) {
                        if (step.status != BatchStatus.COMPLETED && step.failureExceptions.isNotEmpty()) {
                            val errors = step.failureExceptions.joinToString("\n") { it.message ?: "" }
                            append("❌ ${step.stepName}: ${truncate(errors, 500)}\n")
                        }
                    }
                }
            }

        val color = if (success) 0x2ECC71 else 0xE74C3C
        val title = if (success) "IGDB 동기화 배치 완료" else "IGDB 동기화 배치 실패"
        send(formatEmbed(title, color, description))
    }

    private fun isEnabled() = webhookUrl.isNotBlank()

    private fun send(jsonBody: String) {
        try {
            restClient
                .post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            log.warn("Discord 알림 전송 실패: {}", e.message)
        }
    }

    private fun formatEmbed(
        title: String,
        color: Int,
        description: String,
    ): String {
        val escapedTitle = escapeJson(title)
        val escapedDesc = escapeJson(description)
        return """{"embeds":[{"title":"$escapedTitle","description":"$escapedDesc","color":$color}]}"""
    }

    private fun escapeJson(text: String) =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

    private fun formatTime(time: LocalDateTime?): String = time?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) ?: "-"

    private fun formatDuration(
        start: LocalDateTime?,
        end: LocalDateTime?,
    ): String {
        if (start == null || end == null) return "-"
        val duration = Duration.between(start, end)
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()
        return when {
            hours > 0 -> "${hours}시간 ${minutes}분 ${seconds}초"
            minutes > 0 -> "${minutes}분 ${seconds}초"
            else -> "${seconds}초"
        }
    }

    private fun truncate(
        text: String?,
        maxLength: Int,
    ): String {
        if (text == null) return ""
        return if (text.length <= maxLength) text else text.substring(0, maxLength) + "..."
    }
}
