package com.back.domain.member.memberGame.importgame.dto

data class ImportMatchResponse(
    val matches: List<ImportMatchCandidate>,
    val totalRequested: Int,
    val totalMatched: Int,
    val totalAlreadyInLibrary: Int,
)
