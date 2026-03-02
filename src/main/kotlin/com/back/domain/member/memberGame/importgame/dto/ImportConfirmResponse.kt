package com.back.domain.member.memberGame.importgame.dto

data class ImportConfirmResponse(
    val totalAdded: Int,
    val totalSkippedDuplicate: Int,
    val totalFailed: Int,
    val results: List<ImportResultItem>,
)
