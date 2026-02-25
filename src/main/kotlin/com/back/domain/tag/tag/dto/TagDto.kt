package com.back.domain.tag.tag.dto

import com.back.domain.tag.tag.entity.Tag

data class TagDto(
    val id: Int,
    val content: String,
) {
    // 자바 레코드의 생성자(Tag tag)를 부 생성자로 구현
    constructor(tag: Tag) : this(
        id = tag.id,
        content = tag.content,
    )
}
