package com.back.domain.member.memberGame.repository

import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.entity.MemberGame
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface MemberGameRepository : JpaRepository<MemberGame, Int> {
    fun findByMemberIdAndGameId(
        memberId: Int,
        gameId: Int,
    ): MemberGame?

    fun findByMemberId(
        memberId: Int,
        pageable: Pageable,
    ): Page<MemberGame>

    fun findByMemberIdAndStatus(
        memberId: Int,
        status: StatusEnum,
        pageable: Pageable,
    ): Page<MemberGame>

    fun findByMemberIdAndPlatformIdIn(
        memberId: Int,
        platformIds: List<Long>,
        pageable: Pageable,
    ): Page<MemberGame>

    fun findByMemberIdAndStatusAndPlatformIdIn(
        memberId: Int,
        status: StatusEnum,
        platformIds: List<Long>,
        pageable: Pageable,
    ): Page<MemberGame>

    fun findAllByMemberId(memberId: Int): List<MemberGame>
}
