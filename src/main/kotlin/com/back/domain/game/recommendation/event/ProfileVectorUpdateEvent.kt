package com.back.domain.game.recommendation.event

data class ProfileVectorUpdateEvent(
    val memberId: Int,
    val reason: String,
)
