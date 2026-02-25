package com.back.domain.tag.tag.repository

import com.back.domain.tag.tag.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TagRepository : JpaRepository<Tag, Int> {
    // 스프링 데이터 JPA의 명명 규칙에 따라 Content의 C를 대문자로 쓰는 것이 관례입니다.
    fun findByContent(content: String): Optional<Tag>
}
