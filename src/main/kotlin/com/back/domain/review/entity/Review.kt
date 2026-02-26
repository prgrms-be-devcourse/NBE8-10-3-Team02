package com.back.domain.review.entity

import com.back.domain.game.game.entity.Game
import com.back.domain.member.member.entity.Member
import com.back.global.exception.ServiceException
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
class Review(
    var title: String,
    var content: String,
    var rating: Double,
    @ManyToOne(fetch = FetchType.LAZY)
    val author: Member?,
    @ManyToOne(fetch = FetchType.LAZY)
    val game: Game,
) : BaseEntity() {
    @CreatedDate
    @Column(updatable = false)
    var createDate: LocalDateTime? = null

    @LastModifiedDate
    var modifyDate: LocalDateTime? = null

    fun modify(
        title: String,
        content: String,
        rating: Double,
    ) {
        this.title = title
        this.content = content
        this.rating = rating
    }

    fun checkActorCanModify(actor: Member) {
        if (requireNotNull(author).id != actor.id) throw ServiceException("403-1", "${id}번 리뷰 수정권한이 없습니다.")
    }

    fun checkActorCanDelete(actor: Member) {
        if (requireNotNull(author).id != actor.id) throw ServiceException("403-2", "${id}번 리뷰 삭제권한이 없습니다.")
    }
}
