package com.back.domain.review.dto

import com.back.domain.review.entity.Review
import java.time.LocalDateTime

data class ReviewDto(
    val id: Int,
    val title: String,
    val createDate: LocalDateTime?,
    val modifyDate: LocalDateTime?,
    val authorId: Int?,
    val authorNickName: String?,
    val gameId: Int,
    val gameName: String?,
    val content: String,
    val rating: Double,
) {
    constructor(review: Review) : this(
        id = review.id,
        title = review.title,
        createDate = review.createDate,
        modifyDate = review.modifyDate,
        authorId = review.author?.id,
        authorNickName = review.author?.nickname,
        gameId = review.game.getId(),
        gameName = review.game.getName(),
        content = review.content,
        rating = review.rating,
    )
}
