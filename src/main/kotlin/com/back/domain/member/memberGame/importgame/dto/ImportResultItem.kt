package com.back.domain.member.memberGame.importgame.dto

data class ImportResultItem(
    val igdbId: Long,
    val gameName: String?,
    val status: String,
) {
    companion object {
        const val STATUS_ADDED = "ADDED"
        const val STATUS_DUPLICATE = "DUPLICATE"
        const val STATUS_ERROR = "ERROR"
    }
}
