package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.CompanyRole
import com.back.domain.game.game.entity.GameCompany
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GameCompanyRepository : JpaRepository<GameCompany, Long> {
    @Query(
        """
        select c.name
        from GameCompany gc
        join gc.company c
        where gc.game.id = :gameId and gc.role = :role
        order by c.name
    """,
    )
    fun findCompanyNamesByGameIdAndRole(
        @Param("gameId") gameId: Long,
        @Param("role") role: CompanyRole,
    ): List<String>

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM GameCompany gc WHERE gc.game.id IN :gameIds")
    fun deleteByGameIdIn(
        @Param("gameIds") gameIds: Collection<Long>,
    )
}
