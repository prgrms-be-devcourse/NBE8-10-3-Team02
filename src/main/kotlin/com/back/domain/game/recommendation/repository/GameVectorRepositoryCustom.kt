package com.back.domain.game.recommendation.repository

interface GameVectorRepositoryCustom {
    fun bulkUpdateFeatureVectors(gameVectorMap: Map<Long, String>)

    fun applyStagingToGameAndTruncate()
}
