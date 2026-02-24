package com.back.domain.tag.postTag.entity

import com.back.domain.post.post.entity.Post
import com.back.domain.tag.tag.entity.Tag
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "post_tag",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["post_id", "tag_id"])
    ]
)
class PostTag(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id") // 명시적 조인 컬럼 지정 권장
    var post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    var tag: Tag
) : BaseEntity() {

    // JPA를 위한 protected 기본 생성자 (NoArg 플러그인이 있다면 생략 가능)
    protected constructor() : this(
        post = Post(), // 지난번 해결책처럼 Java Member/Post의 기본 생성자가 public이어야 합니다
        tag = Tag()
    )
}