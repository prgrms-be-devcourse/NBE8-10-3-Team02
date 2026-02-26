package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.GameKeyword
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GameKeywordRepository : JpaRepository<GameKeyword, Long> {
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM GameKeyword gk WHERE gk.game.id IN :gameIds")
    fun deleteByGameIdIn(
        @Param("gameIds") gameIds: Collection<Long>,
    )

    fun findByGameId(gameId: Long): List<GameKeyword>

    @Query(
        value = "SELECT gk.keyword_id FROM game_keyword gk GROUP BY gk.keyword_id ORDER BY COUNT(*) DESC LIMIT 100",
        nativeQuery = true,
    )
    fun findTop100KeywordIdsByFrequency(): List<Long>
}
