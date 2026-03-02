package com.back.domain.member.memberGame.importgame.dto

data class ImportMatchCandidate(
    val originalName: String,
    val igdbId: Long?,
    val matchedName: String?,
    val coverImageId: String?,
    val confidence: Double,
    val alreadyInLibrary: Boolean,
    val suggestedPlatform: String?,
    val alternatives: List<AlternativeMatch>,
)
