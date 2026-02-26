package com.back.domain.game.gameLike.service

import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.gameLike.entity.GameLike
import com.back.domain.game.gameLike.repository.GameLikeRepository
import com.back.domain.game.recommendation.event.ProfileVectorUpdateEvent
import com.back.domain.member.member.entity.Member
import com.back.global.exception.ServiceException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameLikeService(
    private val gameLikeRepository: GameLikeRepository,
    private val gameRepository: GameRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun toggleLike(
        member: Member,
        igdbId: Long,
    ): Boolean {
        val game =
            gameRepository
                .findByIgdbId(igdbId)
                .orElseThrow { ServiceException("404-1", "게임을 찾을 수 없습니다. igdbId$igdbId") }

        val existingLike = gameLikeRepository.findByMemberAndGame(member, game)

        val liked: Boolean
        if (existingLike.isPresent) {
            // 이미 있으면 취소
            gameLikeRepository.delete(existingLike.get())
            gameRepository.decrementLikeCount(game.id!!)
            liked = false
        } else {
            // 없으면 추가
            gameLikeRepository.save(GameLike.createGameLike(member, game))
            gameRepository.incrementLikeCount(game.id!!)
            liked = true
        }
        eventPublisher.publishEvent(ProfileVectorUpdateEvent(member.id, "toggleLike"))
        return liked
    }

    @Transactional(readOnly = true)
    fun isLiked(
        member: Member?,
        igdbId: Long,
    ): Boolean {
        if (member == null) return false
        return gameLikeRepository.existsByMemberIdAndGameIgdbId(member.id, igdbId)
    }

    @Transactional(readOnly = true)
    fun getLikeCount(igdbId: Long): Long =
        gameRepository
            .findByIgdbId(igdbId)
            .map { it.likeCount }
            .orElse(0L)
}
