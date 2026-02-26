package com.back.domain.member.memberGame.service

import com.back.domain.game.game.entity.Game
import com.back.domain.game.platform.PlatformGroup
import com.back.domain.game.recommendation.event.ProfileVectorUpdateEvent
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.dto.MemberGameAddRequest
import com.back.domain.member.memberGame.dto.MemberGameUpdateRequest
import com.back.domain.member.memberGame.entity.MemberGame
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.domain.review.entity.Review
import com.back.global.exception.ServiceException
import jakarta.validation.Valid
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberGameService(
    private val memberRepository: MemberRepository,
    private val memberGameRepository: MemberGameRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun addToLibrary(
        request: MemberGameAddRequest,
        member: Member,
        game: Game,
    ): MemberGame {
        if (memberGameRepository.findByMemberIdAndGameId(member.id, game.getId()) != null) {
            throw ServiceException("400-2", "이미 라이브러리에 존재하는 게임입니다.")
        }
        val platformId =
            PlatformGroup.getDefaultPlatformId(request.platform)
                ?: throw ServiceException("400-3", "유효하지 않은 플랫폼입니다: ${request.platform}")
        val memberGame = member.addMemberGame(platformId, request.playtime, request.isFavorite, request.status, game)
        eventPublisher.publishEvent(ProfileVectorUpdateEvent(member.id, "addToLibrary"))
        return memberGame
    }

    fun removeFromLibrary(
        member: Member,
        id: Int,
    ): Boolean {
        val removed = member.removeGame(id)
        if (removed) {
            eventPublisher.publishEvent(ProfileVectorUpdateEvent(member.id, "removeFromLibrary"))
        }
        return removed
    }

    fun findByMemberAndGame(
        memberId: Int,
        gameId: Int,
    ): MemberGame =
        memberGameRepository.findByMemberIdAndGameId(memberId, gameId)
            ?: throw ServiceException("404", "MemberGame not found")

    fun findByMemberId(
        memberId: Int,
        pageable: Pageable,
    ): Page<MemberGame> = memberGameRepository.findByMemberId(memberId, pageable)

    fun findByMemberIdWithFilters(
        memberId: Int,
        status: StatusEnum?,
        platformGroupName: String?,
        pageable: Pageable,
    ): Page<MemberGame> {
        val platformIds = platformGroupName?.let { PlatformGroup.getPlatformIds(it) }
        return when {
            status != null && !platformIds.isNullOrEmpty() ->
                memberGameRepository.findByMemberIdAndStatusAndPlatformIdIn(memberId, status, platformIds, pageable)
            status != null ->
                memberGameRepository.findByMemberIdAndStatus(memberId, status, pageable)
            !platformIds.isNullOrEmpty() ->
                memberGameRepository.findByMemberIdAndPlatformIdIn(memberId, platformIds, pageable)
            else ->
                memberGameRepository.findByMemberId(memberId, pageable)
        }
    }

    @Transactional
    fun updateMemberGame(
        memberGameId: Int,
        memberId: Int,
        @Valid request: MemberGameUpdateRequest,
    ): MemberGame {
        val memberGame =
            memberGameRepository
                .findById(memberGameId)
                .orElseThrow { ServiceException("404", "Game not found") }
        if (memberGame.member.id != memberId) {
            throw ServiceException("403", "Not your game")
        }
        request.status?.let { memberGame.status = it }
        request.playtime?.let { memberGame.playtime = it }
        request.isFavorite?.let { memberGame.isFavorite = it }
        request.platform?.let { memberGame.setPlatformByGroupName(it) }
        eventPublisher.publishEvent(ProfileVectorUpdateEvent(memberId, "updateMemberGame"))
        return memberGame
    }

    @Transactional
    fun updateReview(
        memberId: Int,
        gameId: Int,
        review: Review,
    ): MemberGame {
        val memberGame =
            memberGameRepository.findByMemberIdAndGameId(memberId, gameId)
                ?: throw ServiceException("404", "MemberGame not found")
        memberGame.review = review
        return memberGame
    }
}
