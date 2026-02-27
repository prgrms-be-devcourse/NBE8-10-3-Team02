package com.back.domain.member.memberGame.dto

import com.back.domain.member.memberGame.StatusEnum

data class MemberGameUpdateRequest(
    val platform: String?,
    val playtime: Double?,
    val isFavorite: Boolean?,
    val status: StatusEnum?,
)
