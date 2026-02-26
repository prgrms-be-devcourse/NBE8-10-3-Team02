package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.GameGenre
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GameGenreRepository : JpaRepository<GameGenre, Long> {
    @Query(
        """
        select g.name
        from GameGenre gg
        join gg.genre g
        where gg.game.id = :gameId
        order by g.name
    """,
    )
    fun findGenreNamesByGameId(
        @Param("gameId") gameId: Long,
    ): List<String>

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM GameGenre gg WHERE gg.game.id IN :gameIds")
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
        value = "INSERT INTO game_genre(game_id, genre_id) VALUES (:gameId, :genreId) ON CONFLICT DO NOTHING",
        nativeQuery = true,
    )
    fun insertIgnore(
        @Param("gameId") gameId: Long,
        @Param("genreId") genreId: Long,
    ): Long

    fun findByGameId(gameId: Long): List<GameGenre>
}
