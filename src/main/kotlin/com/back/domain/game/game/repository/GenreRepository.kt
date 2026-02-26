package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Genre
import org.springframework.data.jpa.repository.JpaRepository

interface GenreRepository : JpaRepository<Genre, Long> {
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<Genre>
}
