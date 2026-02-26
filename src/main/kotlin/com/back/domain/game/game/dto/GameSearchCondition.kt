package com.back.domain.game.game.dto

data class GameSearchCondition(
    var query: String? = null, // 자연어검색
    var genreIds: List<Long>? = null,
    var platformCode: String? = null, // 대표코드만
    var platformIgdbIds: List<Long>? = null, // 서버에서 검색용
    var page: Int? = null, // 1부터 시작
    var size: Int? = null, // 한 페이지 개수
)
