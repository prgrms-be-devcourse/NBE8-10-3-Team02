package com.back.domain.game.game.dto

import com.back.global.igdb.dto.IgdbGameSummaryDto
import com.back.global.igdb.util.IgdbImageUtil
import com.back.standard.util.TimeUt
import java.time.LocalDate
import java.util.*


data class GameSearchResponse(
    val igdbId: Long,
    val name: String?,
    val imageUrl: String?,
    val firstReleaseDate: LocalDate?,
    val genres: MutableList<String?>?,
    val platforms: MutableList<String?>? // developerName 추가예정
) {
    companion object {
        //    IGDB 조회용
        fun fromDto(
            d: IgdbGameSummaryDto,
            genreMap: MutableMap<Long?, String?>,
            platformMap: MutableMap<Long?, String?>
        ): GameSearchResponse {
            return GameSearchResponse(
                d.id,
                d.name,
                IgdbImageUtil.cover(
                    if (d.cover != null) d.cover.imageId else null
                ),
                TimeUt.epoch.toLocalDate(d.firstReleaseDateEpochSeconds),  // 장르 null 체크
                if (d.genres == null) mutableListOf<String?>() else d.genres.stream()
                    .map<String?> { key: Long? -> genreMap.get(key) }
                    .filter { obj: String? -> Objects.nonNull(obj) }
                    .toList(),  // 플랫폼 null 체크
                if (d.platforms == null) mutableListOf<String?>() else d.platforms.stream()
                    .map<String?> { key: Long? -> platformMap.get(key) }
                    .filter { obj: String? -> Objects.nonNull(obj) }
                    .toList()
            )
        }
    }
}
