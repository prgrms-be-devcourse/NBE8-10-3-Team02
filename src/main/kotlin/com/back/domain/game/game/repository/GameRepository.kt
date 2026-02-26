package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Game
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface GameRepository : JpaRepository<Game, Long> {
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<Game>

    fun findByIgdbId(igdbId: Long): Optional<Game>

    // 자체 서비스 인기 (조회수 기준)
    fun findTop10ByOrderByViewCountDesc(): List<Game>

    // 자체 서비스 인기 (좋아요 기준)
    fun findTop10ByOrderByLikeCountDesc(): List<Game>

    // bulk update
    @Modifying
    @Query("UPDATE Game g SET g.viewCount = g.viewCount + :delta WHERE g.igdbId = :igdbId")
    fun incrementViewCount(
        @Param("igdbId") igdbId: Long,
        @Param("delta") delta: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Game g SET g.likeCount = g.likeCount + 1 WHERE g.id = :gameId")
    fun incrementLikeCount(
        @Param("gameId") gameId: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Game g SET g.likeCount = g.likeCount - 1 WHERE g.id = :gameId AND g.likeCount > 0")
    fun decrementLikeCount(
        @Param("gameId") gameId: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Game g SET g.reviewCount = g.reviewCount + 1 WHERE g.id = :gameId")
    fun incrementReviewCount(
        @Param("gameId") gameId: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Game g SET g.reviewCount = g.reviewCount - 1 WHERE g.id = :gameId AND g.reviewCount > 0")
    fun decrementReviewCount(
        @Param("gameId") gameId: Long,
    )

    @Query("SELECT MAX(g.lastFetchedAt) FROM Game g")
    fun findMaxLastFetchedAt(): Optional<Instant>

    // IGDB fallback: 이름 ILIKE 검색 (Circuit Breaker fallback 용)
    @Query("SELECT g FROM Game g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY g.likeCount DESC")
    fun findByNameContainingIgnoreCaseLimited(
        @Param("name") name: String,
        pageable: Pageable,
    ): List<Game>

    // IGDB fallback: 좋아요 기준 인기 게임 (Circuit Breaker fallback 용)
    fun findByOrderByLikeCountDesc(pageable: Pageable): List<Game>
}
