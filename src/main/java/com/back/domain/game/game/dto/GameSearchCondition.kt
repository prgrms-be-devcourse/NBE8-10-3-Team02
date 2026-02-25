package com.back.domain.game.game.dto

data class GameSearchCondition(
    var query: String? = null,
    var genreIds: MutableList<Long?>? = null,
    var platformCode: String? = null,
    var platformIgdbIds: MutableList<Long?>? = null,
    var page: Int? = null,
    var size: Int? = null,
)
