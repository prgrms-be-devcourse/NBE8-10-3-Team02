package com.back.domain.tag.postTag.repository

import com.back.domain.tag.postTag.entity.PostTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostTagRepository : JpaRepository<PostTag, Int> {
    // 필요한 쿼리 메서드가 있다면 여기에 추가하세요.
}