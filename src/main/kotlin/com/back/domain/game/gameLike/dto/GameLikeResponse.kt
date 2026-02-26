package com.back.domain.game.gameLike.dto

data class GameLikeResponse(
    val igdbId: Long,
    val liked: Boolean,
    val likeCount: Long,
) {
    companion object {
        fun from(
            igdbId: Long,
            liked: Boolean,
            likeCount: Long,
        ) = GameLikeResponse(igdbId, liked, likeCount)
    }
}
