package com.back.domain.game.recommendation.repository

import jakarta.persistence.EntityManager
import org.hibernate.Session

class GameVectorRepositoryImpl(
    private val entityManager: EntityManager,
) : GameVectorRepositoryCustom {
    companion object {
        private const val BATCH_SIZE = 1000
    }

    /**
     * Chunk마다 호출: staging에만 UPSERT 한다.
     * (TRUNCATE/UPDATE JOIN은 여기서 하지 않음)
     */
    override fun bulkUpdateFeatureVectors(gameVectorMap: Map<Long, String>) {
        if (gameVectorMap.isEmpty()) return

        entityManager.flush()

        entityManager.unwrap(Session::class.java).doWork { connection ->
            val upsertSql =
                "INSERT INTO game_vector_staging (game_id, feature_vector) " +
                    "VALUES (?, ?) " +
                    "ON CONFLICT (game_id) DO UPDATE " +
                    "SET feature_vector = EXCLUDED.feature_vector"

            connection.prepareStatement(upsertSql).use { ps ->
                var count = 0
                for ((gameId, vector) in gameVectorMap) {
                    ps.setLong(1, gameId)
                    ps.setString(2, vector)
                    ps.addBatch()

                    if (++count % BATCH_SIZE == 0) {
                        ps.executeBatch()
                        ps.clearBatch()
                    }
                }
                ps.executeBatch()
            }
        }
    }

    /**
     * Step 종료 시 1회 호출: staging 내용을 game으로 반영하고 staging 비움.
     */
    override fun applyStagingToGameAndTruncate() {
        entityManager.flush()

        entityManager.unwrap(Session::class.java).doWork { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute(
                    "UPDATE game g " +
                        "SET feature_vector = cast(s.feature_vector as vector) " +
                        "FROM game_vector_staging s " +
                        "WHERE g.id = s.game_id",
                )
                stmt.execute("TRUNCATE game_vector_staging")
            }
        }
    }
}
