package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Platform
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PlatformRepository : JpaRepository<Platform, Long> {
    fun findByIgdbId(igdbId: Long): Optional<Platform>

    fun findByIgdbIdIn(igdbIds: List<Long>): List<Platform>
}
