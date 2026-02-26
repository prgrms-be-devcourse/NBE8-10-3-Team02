package com.back.domain.game.recommendation.dto

data class GameRecommendationResponse(
    val gameId: Int,
    val name: String?,
    val coverImageId: String?,
    val score: Double,
    val similarity: Double,
)
