package com.back.domain.member.memberGame.importgame.dto

import jakarta.validation.constraints.NotNull

data class ImportGameItem(
    @field:NotNull val igdbId: Long,
    val platform: String?,
)
