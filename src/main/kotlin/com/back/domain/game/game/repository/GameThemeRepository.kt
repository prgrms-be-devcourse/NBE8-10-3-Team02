package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.GameTheme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GameThemeRepository : JpaRepository<GameTheme, Long> {
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM GameTheme gt WHERE gt.game.id IN :gameIds")
    fun deleteByGameIdIn(
        @Param("gameIds") gameIds: Collection<Long>,
    )

    fun findByGameId(gameId: Long): List<GameTheme>
}
