package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Theme
import org.springframework.data.jpa.repository.JpaRepository

interface ThemeRepository : JpaRepository<Theme, Long> {
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<Theme>
}
