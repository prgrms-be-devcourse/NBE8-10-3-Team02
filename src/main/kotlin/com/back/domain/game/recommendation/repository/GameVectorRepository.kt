package com.back.domain.game.recommendation.repository

import com.back.domain.game.game.entity.Game
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GameVectorRepository :
    JpaRepository<Game, Long>,
    GameVectorRepositoryCustom {
    @Query(value = "SELECT g.id FROM game g WHERE g.feature_vector IS NULL", nativeQuery = true)
    fun findGameIdsWithoutVector(pageable: Pageable): List<Long>

    @Query(
        value = """
        SELECT g.id, g.feature_vector::text
        FROM member_game mg JOIN game g ON mg.game_id = g.id
        WHERE mg.member_id = :memberId AND g.feature_vector IS NOT NULL
        """,
        nativeQuery = true,
    )
    fun findFeatureVectorsByMemberId(
        @Param("memberId") memberId: Int,
    ): List<Array<Any?>>

    @Query(
        value = """
        SELECT g.id, g.name, g.cover_image_id, g.aggregated_rating, g.like_count,
               1 - (g.feature_vector <=> cast(:profileVector as vector)) AS similarity
        FROM game g
        WHERE g.feature_vector IS NOT NULL
          AND g.id NOT IN (:excludeGameIds)
        ORDER BY g.feature_vector <=> cast(:profileVector as vector)
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findSimilarGamesByUserVector(
        @Param("profileVector") profileVector: String,
        @Param("excludeGameIds") excludeGameIds: List<Long>,
        @Param("limit") limit: Int,
    ): List<Array<Any?>>

    @Query(
        value = "SELECT feature_vector::text FROM game WHERE igdb_id = :igdbId",
        nativeQuery = true,
    )
    fun findFeatureVectorByIgdbId(
        @Param("igdbId") igdbId: Long,
    ): String?

    @Query(
        value = """
        SELECT g.igdb_id, g.name, g.cover_image_id, g.aggregated_rating, g.like_count,
               1 - (g.feature_vector <=> cast(:targetVector as vector)) AS similarity
        FROM game g
        WHERE g.feature_vector IS NOT NULL
          AND g.igdb_id != :igdbId
        ORDER BY g.feature_vector <=> cast(:targetVector as vector)
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findSimilarGamesByIgdbId(
        @Param("targetVector") targetVector: String,
        @Param("igdbId") igdbId: Long,
        @Param("limit") limit: Int,
    ): List<Array<Any?>>
}
