package com.back.global.batch.tasklet

object VectorBatchSql {
    const val VECTOR_INDEX_NAME = "ix_game_feature_vector"

    const val DROP_VECTOR_INDEX = "DROP INDEX IF EXISTS $VECTOR_INDEX_NAME"

    const val CREATE_VECTOR_INDEX =
        "CREATE INDEX IF NOT EXISTS $VECTOR_INDEX_NAME" +
            " ON game USING hnsw (feature_vector vector_cosine_ops)"

    const val TRUNCATE_VECTOR_STAGING = "TRUNCATE game_vector_staging"

    const val APPLY_STAGING_TO_GAME =
        "UPDATE game g " +
            "SET feature_vector = cast(s.feature_vector as vector) " +
            "FROM game_vector_staging s " +
            "WHERE g.id = s.game_id"
}
