package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.GameMode
import org.springframework.data.jpa.repository.JpaRepository

interface GameModeRepository : JpaRepository<GameMode, Long> {
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<GameMode>
}
