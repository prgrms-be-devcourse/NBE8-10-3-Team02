package com.back.domain.member.memberGame.dto

import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.entity.MemberGame
import com.fasterxml.jackson.annotation.JsonProperty

data class MemberGameDto(
    val id: Int,
    val platform: String?,
    val playtime: Double,
    @get:JsonProperty("isFavorite") val isFavorite: Boolean,
    val status: StatusEnum?,
    val gameId: Int,
    val igdbId: Long?,
    val gameName: String?,
    val coverImageId: String?,
    val reviewId: Int?,
    val rating: Double?,
) {
    constructor(memberGame: MemberGame) : this(
        id = memberGame.id,
        platform = memberGame.platformGroupName,
        playtime = memberGame.playtime,
        isFavorite = memberGame.isFavorite,
        status = memberGame.status,
        gameId = memberGame.game.getId(),
        igdbId = memberGame.game.getIgdbId(),
        gameName = memberGame.game.getName(),
        coverImageId = memberGame.game.getCoverImageId(),
        reviewId = memberGame.review?.id,
        rating = memberGame.review?.getRating(),
    )
}
