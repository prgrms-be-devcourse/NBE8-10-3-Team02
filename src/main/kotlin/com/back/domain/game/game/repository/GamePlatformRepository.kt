package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.GamePlatform
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GamePlatformRepository : JpaRepository<GamePlatform, Long> {
    @Query(
        """
        select p.name
        from GamePlatform gp
        join gp.platform p
        where gp.game.id = :gameId
        order by p.name
    """,
    )
    fun findPlatformNamesByGameId(
        @Param("gameId") gameId: Long,
    ): List<String>

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM GamePlatform gp WHERE gp.game.id IN :gameIds")
    fun deleteByGameIdIn(
        @Param("gameIds") gameIds: Collection<Long>,
    )

    /**
     * native insert를 해주면 1차 캐시를 업데이트 하지 않는다.
     * flushAutomatically = true : 앞에서 쌓인 JPA 변경사항을 DB에 먼저 밀어넣음(native insert가 FK 위반 안 나게)
     * clearAutomatically = true : 영속성 컨텍스트를 비워서, 같은 트랜잭션에서 조회해도 예전 상태 조회가 안되도록 함
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = "INSERT INTO game_platform(game_id, platform_id) VALUES (:gameId, :platformId) ON CONFLICT DO NOTHING",
        nativeQuery = true,
    )
    fun insertIgnore(
        @Param("gameId") gameId: Long,
        @Param("platformId") platformId: Long,
    ): Long
}
