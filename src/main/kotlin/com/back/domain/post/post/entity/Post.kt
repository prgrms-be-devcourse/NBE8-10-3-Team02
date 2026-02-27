package com.back.domain.post.post.entity

import com.back.domain.member.member.entity.Member
import com.back.domain.post.postComment.entity.PostComment
import com.back.domain.tag.postTag.entity.PostTag
import com.back.domain.tag.tag.entity.Tag
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
class Post(
    @ManyToOne(fetch = FetchType.LAZY)
    var author: Member,
    var title: String,
    @Column(columnDefinition = "TEXT")
    var content: String,
) : BaseEntity() {
    @CreatedDate
    @Column(updatable = false)
    var createDate: LocalDateTime? = null
        protected set

    @LastModifiedDate
    var modifyDate: LocalDateTime? = null
        protected set

    // 기본 생성자 (JPA용)
    constructor() : this(
        // 자바 Member에 있는 생성자 중 아무거나 형식을 맞춥니다.
        Member("temp", "temp", "temp"),
        "",
        "",
    )

    @OneToMany(mappedBy = "post", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    var comments: MutableList<PostComment> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var postTags: MutableList<PostTag> = mutableListOf()
        protected set

    @Column(columnDefinition = "integer default 0", nullable = false)
    var viewCount: Int = 0
        protected set

    fun increaseView() {
        this.viewCount += 1
    }

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var postLikes: MutableList<PostLike> = mutableListOf()
        protected set

    companion object {
        fun create(
            author: Member,
            title: String,
            content: String,
        ): Post = Post(author, title, content)
    }

    fun modify(
        title: String,
        content: String,
    ) {
        this.title = title
        this.content = content
    }

    fun addComment(
        author: Member,
        content: String,
    ): PostComment {
        val postComment = PostComment(author, this, content)
        comments.add(postComment)
        return postComment
    }

    fun findCommentById(id: Int): PostComment? = comments.find { it.id == id }

    fun deleteComment(postComment: PostComment?): Boolean = postComment?.let { comments.remove(it) } ?: false

    fun addTag(tag: Tag) {
        val postTag = PostTag(this, tag)
        this.postTags.add(postTag)
    }
}
