package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.GameExternalId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GameExternalIdRepository : JpaRepository<GameExternalId, Long> {
    fun findByPlatformAndExternalIdIn(
        platform: String,
        externalIds: Collection<String>,
    ): List<GameExternalId>

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM GameExternalId gei WHERE gei.game.id IN :gameIds")
    fun deleteByGameIdIn(
        @Param("gameIds") gameIds: Collection<Long>,
    )
}
