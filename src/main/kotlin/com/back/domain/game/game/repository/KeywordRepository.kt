package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Keyword
import org.springframework.data.jpa.repository.JpaRepository

interface KeywordRepository : JpaRepository<Keyword, Long> {
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<Keyword>
}
