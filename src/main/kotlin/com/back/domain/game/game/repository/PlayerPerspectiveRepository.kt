package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.PlayerPerspective
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerPerspectiveRepository : JpaRepository<PlayerPerspective, Long> {
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<PlayerPerspective>
}
