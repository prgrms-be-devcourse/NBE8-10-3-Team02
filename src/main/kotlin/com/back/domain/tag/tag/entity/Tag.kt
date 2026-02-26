package com.back.domain.tag.tag.entity

import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "tag",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["content"]),
    ],
)
class Tag(
    @Column(unique = true, nullable = false)
    var content: String = "",
) : BaseEntity() {
    // JPA를 위한 protected 기본 생성자
    protected constructor() : this("")

    /* 수정 메서드가 필요하다면 주석을 풀고 사용하세요.
    fun modify(content: String) {
        this.content = content
    }
     */
}
