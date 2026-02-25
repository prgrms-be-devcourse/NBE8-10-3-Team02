package com.back.domain.post.dto

import com.back.domain.post.post.entity.Post
import java.time.LocalDateTime

data class PostDto(
    val id: Int,
    val authorId: Int,
    val authorName: String?,
    val title: String,
    val content: String,
    val createDate: LocalDateTime?, // 자바 엔티티의 null 가능성을 수용
    val modifyDate: LocalDateTime?,
    val tags: List<String>,
    val viewCount: Int,
    val likeCount: Long,
) {
    // 자바의 보조 생성자 역할을 하는 companion object나 부 생성자(constructor)
    constructor(post: Post) : this(
        id = post.id,
        authorId = post.author?.id ?: 0,
        authorName = post.author?.nickname, // 롬복 Getter가 있어도 .nickname으로 접근 가능 (안되면 .getNickname())
        title = post.title,
        content = post.content,
        createDate = post.createDate,
        modifyDate = post.modifyDate,
        tags = post.postTags?.map { it.tag.content } ?: emptyList(),
        viewCount = post.viewCount,
        likeCount = post.postLikes?.size?.toLong() ?: 0L,
    )
}
