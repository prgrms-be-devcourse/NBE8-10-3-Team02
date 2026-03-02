package com.back.domain.member.memberGame.importgame.dto

import jakarta.validation.constraints.NotEmpty

data class ImportMatchRequest(
    @field:NotEmpty val gameNames: List<String>,
    val sourcePlatform: String?,
)
