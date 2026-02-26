package com.back.domain.review.repository

import com.back.domain.game.game.entity.Game
import com.back.domain.member.member.entity.Member
import com.back.domain.review.entity.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Int> {
    fun findByAuthorAndGame(
        author: Member,
        game: Game,
    ): Review?

    fun findByAuthorId(authorId: Long): List<Review>

    fun findByGameId(gameId: Long): List<Review>

    fun findByAuthorId(
        authorId: Long,
        pageable: Pageable,
    ): Page<Review>

    fun findByGameId(
        gameId: Long,
        pageable: Pageable,
    ): Page<Review>
}
