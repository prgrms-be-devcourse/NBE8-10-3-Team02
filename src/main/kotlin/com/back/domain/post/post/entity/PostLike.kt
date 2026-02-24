package com.back.domain.post.post.entity

import com.back.domain.member.member.entity.Member
import jakarta.persistence.*

@Entity
class PostLike(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    var post: Post
) {
    // JPA를 위한 기본 생성자
    protected constructor() : this(member = Member(), post = Post(Member(), "", ""))
}