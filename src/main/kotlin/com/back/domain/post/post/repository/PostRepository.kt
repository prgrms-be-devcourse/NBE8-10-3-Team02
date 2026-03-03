package com.back.domain.post.post.repository

import com.back.domain.post.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Int> {
    // PostRepository 또는 PostService 내의 쿼리 부분 수정 예시
    @Query("""
    SELECT DISTINCT p FROM Post p 
    LEFT JOIN p.postTags pt 
    LEFT JOIN pt.tag t 
    WHERE (:kw IS NULL OR p.title LIKE CONCAT('%', CAST(:kw AS string), '%')) 
      AND (:tag IS NULL OR t.content LIKE CONCAT('%', CAST(:tag AS string), '%')) 
    ORDER BY p.id DESC
""")
    fun search(@Param("kw") kw: String?, @Param("tag") tag: String?, pageable: Pageable): Page<Post>
}
