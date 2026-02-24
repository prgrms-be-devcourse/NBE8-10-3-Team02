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
// 1. 롬복 제거 및 생성자 주입 (빨간 줄 해결의 핵심)
@Transactional(readOnly = true)
class GameSearchService(
    private val igdbService: IgdbService,
    private val genreRepository: GenreRepository
) {

    fun search(condition: GameSearchCondition): List<GameSearchResponse> {
        // 1. 입력 값 정규화 및 검증 (get/set 제거 -> 프로퍼티 접근)
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

        // 2. IGDB 검색 (!! 제거)
        val igdbGames = try {
            igdbService.search(condition)
        } catch (e: Exception) {
            throw ServiceException("502-1", "외부 게임 검색 서버 오류")
        }

        // 3. 장르 매핑 (null인 ID들을 완전히 제거하고 넘겨줌)
        val genreIgdbIds = igdbGames
            .flatMap { it?.genres ?: emptyList() }
            .filterNotNull()
            .toSet()


        val genreMap: MutableMap<Long?, String?> = genreRepository.findByIgdbIdIn(genreIgdbIds)
            .associate { (it.igdbId as Long?) to (it.name as String?) }
            .toMutableMap()

        // 4. 플랫폼 수집
        val platformIds = igdbGames
            .flatMap { it?.genres ?: emptyList() } // d가 null일 경우 대비
            .toSet()

        // 플랫폼 맵도 타입을 강제로 맞춰줌
        val platformMap: MutableMap<Long?, String?> = (igdbService.getPlatformNameMap(platformIds) ?: emptyMap())
            .mapKeys { it.key as Long? }
            .mapValues { it.value as String? }
            .toMutableMap()

        // 5. DTO 변환
        return igdbGames.map { d ->

            GameSearchResponse.fromDto(d!!, genreMap, platformMap)
        }
    }
}