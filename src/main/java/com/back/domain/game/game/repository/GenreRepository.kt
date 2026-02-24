package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Genre
import org.springframework.data.jpa.repository.JpaRepository

interface GenreRepository : JpaRepository<Genre, Long> {

    // 주석 처리되어 있던 부분도 코틀린 스타일로 변환하면 다음과 같습니다.
    // fun findByIgdbId(igdbId: Long): Genre?

    /**
     * IGDB ID 목록에 해당하는 장르들을 조회합니다.
     * 코틀린의 List는 자바의 Collection/List를 모두 포괄하며 더 안전합니다.
     */
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<Genre>
}