package com.back.domain.game.game.dto

import com.back.global.igdb.dto.IgdbGameSummaryDto
import com.back.global.igdb.util.IgdbImageUtil
import com.back.standard.util.TimeUt
import java.time.LocalDate



data class GameSearchResponse(
    val igdbId: Long,             // 자바 테스트에서 jsonPath("$.igdbId")로 확인 중이므로 유지
    val name: String?,            // 자바 테스트에서 jsonPath("$.name")으로 확인 중이므로 유지
    val imageUrl: String?,
    val firstReleaseDate: LocalDate?,
    val genres: List<String?>?,   // MutableList보다 List가 자바 호환성이 좋습니다
    val platforms: List<String?>?
) {
    companion object {
        fun fromDto(
            d: IgdbGameSummaryDto,
            genreMap: Map<Long?, String?>,      // MutableMap일 필요 없음
            platformMap: Map<Long?, String?>    // MutableMap일 필요 없음
        ): GameSearchResponse {
            return GameSearchResponse(
                igdbId = d.id,
                name = d.name,
                imageUrl = IgdbImageUtil.cover(d.cover?.imageId),
                firstReleaseDate = TimeUt.epoch.toLocalDate(d.firstReleaseDateEpochSeconds),
                // 코틀린스러운 컬렉션 처리 (stream 대신 mapNotNull 사용)
                genres = d.genres?.mapNotNull { genreMap[it] } ?: emptyList(),
                platforms = d.platforms?.mapNotNull { platformMap[it] } ?: emptyList()
            )
        }
    }
}
