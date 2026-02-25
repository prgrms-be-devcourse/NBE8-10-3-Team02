package com.back.domain.game.game.service

import com.back.domain.game.game.dto.GameSearchCondition
import com.back.domain.game.game.dto.GameSearchResponse
import com.back.domain.game.game.repository.GenreRepository
import com.back.domain.game.platform.PlatformGroup
import com.back.global.exception.ServiceException
import com.back.global.igdb.service.IgdbService
import com.back.global.search.SearchNormalizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GameSearchService(
    private val igdbService: IgdbService,
    private val genreRepository: GenreRepository,
) {
    fun search(condition: GameSearchCondition): List<GameSearchResponse> {
        val query = condition.query
        if (query.isNullOrBlank()) {
            throw ServiceException("400-2", "검색어를 입력해주세요.")
        }
        condition.query = SearchNormalizer.normalize(query)

        val platformCode = condition.platformCode
        val hasPlatformFilter = !platformCode.isNullOrBlank()

        if (hasPlatformFilter) {
            val normalized = platformCode!!.trim().uppercase()
            val mappedIds = PlatformGroup.PLATFORM_MAP[normalized]

            if (mappedIds.isNullOrEmpty()) {
                throw ServiceException("400-1", "지원하지 않는 플랫폼 코드입니다: $normalized")
            }

            condition.platformIgdbIds = mappedIds
        } else {
            condition.platformIgdbIds = null
        }

        val igdbGames =
            try {
                igdbService.search(condition)
            } catch (e: Exception) {
                throw ServiceException("502-1", "외부 게임 검색 서버 오류")
            }

        val genreIgdbIds =
            igdbGames
                .flatMap { it?.genres ?: emptyList() }
                .filterNotNull()
                .toSet()

        val genreMap: MutableMap<Long?, String?> =
            genreRepository
                .findByIgdbIdIn(genreIgdbIds)
                .associate { (it.igdbId as Long?) to (it.name as String?) }
                .toMutableMap()

        val platformIds =
            igdbGames
                .flatMap { it?.genres ?: emptyList() }
                .toSet()

        val platformMap: MutableMap<Long?, String?> =
            (igdbService.getPlatformNameMap(platformIds) ?: emptyMap())
                .mapKeys { it.key as Long? }
                .mapValues { it.value as String? }
                .toMutableMap()

        return igdbGames.map { d ->

            GameSearchResponse.fromDto(d!!, genreMap, platformMap)
        }
    }
}
