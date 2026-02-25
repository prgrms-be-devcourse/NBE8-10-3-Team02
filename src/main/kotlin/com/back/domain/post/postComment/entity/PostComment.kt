package com.back.domain.post.postComment.entity

import com.back.domain.member.member.entity.Member
import com.back.domain.post.post.entity.Post
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
class PostComment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    var author: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    var post: Post,
    @Column(columnDefinition = "TEXT")
    var content: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: PostComment? = null,
) : BaseEntity() {
    // 1. JPA No-Arg Constructor (가짜 인자 최소화)
    protected constructor() : this(Member(), Post(), "")

    @CreatedDate
    @Column(updatable = false)
    var createDate: LocalDateTime = LocalDateTime.now() // null 방지
        protected set

    @LastModifiedDate
    var modifyDate: LocalDateTime = LocalDateTime.now()
        protected set

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    var children: MutableList<PostComment> = mutableListOf()
        protected set

    var deleted: Boolean = false
        protected set

    // 2. 연관관계 편의 메서드 (대댓글 추가 시 양방향 연결)
    fun addChild(child: PostComment) {
        children.add(child)
        child.parent = this
    }

    fun modify(content: String) {
        this.content = content
    }

    fun markAsDeleted() {
        this.content = "삭제된 댓글입니다."
        this.deleted = true
    }
}
