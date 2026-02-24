package com.back.domain.post.postComment.repository

import com.back.domain.post.postComment.entity.PostComment
import org.springframework.data.jpa.repository.JpaRepository

interface PostCommentRepository : JpaRepository<PostComment, Int>