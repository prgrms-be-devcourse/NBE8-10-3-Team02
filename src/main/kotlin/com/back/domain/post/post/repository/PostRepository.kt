package com.back.domain.post.post.repository

import com.back.domain.post.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Int> {
    @Query(
        """
        SELECT DISTINCT p FROM Post p 
        LEFT JOIN p.postTags pt 
        LEFT JOIN pt.tag t 
        WHERE (:kw IS NULL OR p.title LIKE CONCAT('%', :kw, '%')) 
        AND (:tag IS NULL OR t.content LIKE CONCAT('%', :tag, '%'))
    """,
    )
    fun search(
        @Param("kw") kw: String?,
        @Param("tag") tag: String?,
        pageable: Pageable,
    ): Page<Post>
}
