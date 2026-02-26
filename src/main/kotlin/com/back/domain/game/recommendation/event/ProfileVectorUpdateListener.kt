package com.back.domain.game.recommendation.event

import com.back.domain.game.recommendation.service.UserProfileVectorService
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Component
class ProfileVectorUpdateListener(
    private val userProfileVectorService: UserProfileVectorService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scheduler = Executors.newScheduledThreadPool(4)
    private val pendingTasks = ConcurrentHashMap<Int, ScheduledFuture<*>>()

    companion object {
        private const val DEBOUNCE_SECONDS = 30L
    }

    @EventListener
    fun handleProfileVectorUpdate(event: ProfileVectorUpdateEvent) {
        val memberId = event.memberId
        log.debug("프로필 벡터 갱신 이벤트 수신: memberId={}, reason={}", memberId, event.reason)

        pendingTasks.compute(memberId) { _, existingFuture ->
            existingFuture?.cancel(false).also {
                if (existingFuture != null) log.debug("디바운싱: 기존 예약 취소 memberId={}", memberId)
            }
            scheduler.schedule({
                pendingTasks.remove(memberId)
                try {
                    userProfileVectorService.calculateAndSaveProfileVector(memberId)
                } catch (e: Exception) {
                    log.warn("프로필 벡터 갱신 실패: memberId={}, error={}", memberId, e.message)
                }
            }, DEBOUNCE_SECONDS, TimeUnit.SECONDS)
        }
    }

    @PreDestroy
    fun shutdown() {
        scheduler.shutdown()
    }
}
