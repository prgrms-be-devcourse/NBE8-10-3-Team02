package com.back.domain.member.memberGame.dto

import com.back.domain.member.memberGame.StatusEnum
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class MemberGameAddRequest(
    @field:NotBlank val platform: String,
    @field:DecimalMin("0.0") val playtime: Double,
    val isFavorite: Boolean,
    @field:NotNull val status: StatusEnum,
    val gameId: Int
)
