package com.back.domain.member.memberGame.importgame.dto

import jakarta.validation.constraints.NotEmpty

data class ImportConfirmRequest(
    @field:NotEmpty val games: List<ImportGameItem>,
)
