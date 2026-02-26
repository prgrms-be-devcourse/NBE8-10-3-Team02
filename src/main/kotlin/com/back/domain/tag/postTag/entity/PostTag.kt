package com.back.domain.tag.postTag.entity
import com.back.domain.post.post.entity.Post
import com.back.domain.tag.tag.entity.Tag
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "post_tag",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["post_id", "tag_id"]),
    ],
)
class PostTag(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    var post: Post,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    var tag: Tag,
) : BaseEntity() {
    protected constructor() : this(
        post = Post(),
        tag = Tag(),
    )
}
