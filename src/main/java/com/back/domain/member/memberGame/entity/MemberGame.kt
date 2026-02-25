package com.back.domain.member.memberGame.entity

import com.back.domain.game.game.entity.Game
import com.back.domain.game.platform.PlatformGroup
import com.back.domain.member.member.entity.Member
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.review.entity.Review
import com.back.global.exception.ServiceException
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "member_game")
open class MemberGame(
    var platformId: Long?,
    var playtime: Double,
    var isFavorite: Boolean,
    @field:Enumerated(EnumType.STRING) var status: StatusEnum?,
    @field:ManyToOne(fetch = FetchType.LAZY) var member: Member,
    @field:ManyToOne(fetch = FetchType.LAZY) var game: Game,
) : BaseEntity() {
    @OneToOne(fetch = FetchType.LAZY)
    var review: Review? = null

    val platformGroupName: String?
        get() = PlatformGroup.getGroupName(platformId)

    fun checkActorCanAccess(actor: Member) {
        if (member.id != actor.id) {
            throw ServiceException("403-1", "${member.id}번 게임에 대한 권한이 없습니다.")
        }
    }

    fun setPlatformByGroupName(groupName: String?) {
        this.platformId = PlatformGroup.getDefaultPlatformId(groupName)
    }
}
