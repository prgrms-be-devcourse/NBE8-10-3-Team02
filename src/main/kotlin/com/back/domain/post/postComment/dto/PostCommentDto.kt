package com.back.domain.post.postComment.dto

import com.back.domain.post.postComment.entity.PostComment
import java.time.LocalDateTime

data class PostCommentDto(
    val id: Int,
    val authorId: Int,
    val authorName: String?,
    val content: String,
    val children: List<PostCommentDto>,
    val parentId: Int?,
    val deleted: Boolean,
    val createdDate: LocalDateTime?,
    val modifyDate: LocalDateTime?,
    val postId: Int
) {
    // 자바의 생성자 로직을 부 생성자(constructor)로 구현
    constructor(postComment: PostComment) : this(
        id = postComment.id,
        authorId = postComment.author?.id ?: 0,
        authorName = postComment.author?.nickname,
        content = if (postComment.isDeleted) "삭제된 댓글입니다." else postComment.content,
        children = postComment.children.map { PostCommentDto(it) },
        parentId = postComment.parent?.id,
        deleted = postComment.isDeleted,
        createdDate = postComment.createDate,
        modifyDate = postComment.modifyDate,
        postId = postComment.post?.id ?: 0
    )
}