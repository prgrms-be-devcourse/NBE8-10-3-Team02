package com.back.domain.post.post.repository

import com.back.domain.member.member.entity.Member
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun findByMemberAndPost(
        member: Member,
        post: Post,
    ): Optional<PostLike>

    fun countByPost(post: Post): Long
}
