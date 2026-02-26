package com.back.domain.game.game.service

import com.back.domain.game.game.dto.GameSearchCondition
import com.back.domain.game.game.dto.GameSearchResponse
import com.back.domain.game.game.repository.GenreRepository
import com.back.domain.game.platform.PlatformGroup.PLATFORM_MAP
import com.back.global.exception.ServiceException
import com.back.global.igdb.service.IgdbService
import com.back.global.search.SearchNormalizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GameSearchService(
    private val igdbService: IgdbService,
    private val genreRepository: GenreRepository
) {

    fun search(condition: GameSearchCondition): List<GameSearchResponse> {
        // 1. 입력 값 정규화 및 검증
        val query = condition.query
        if (query.isNullOrBlank()) {
            throw ServiceException("400-2", "검색어를 입력해주세요.")
        }

        condition.query = SearchNormalizer.normalize(query)

        val platformCode = condition.platformCode
        if (!platformCode.isNullOrBlank()) {
            val normalized = platformCode.trim().uppercase()
            // PLATFORM_MAP에서 값을 가져올 때 null 체크
            val mappedIds = PLATFORM_MAP[normalized]
                ?: throw ServiceException("400-1", "지원하지 않는 플랫폼 코드입니다: $normalized")

            condition.platformIgdbIds = mappedIds.toMutableList()
        } else {
            condition.platformIgdbIds = null
        }

        // IGDB 검색
        val igdbGames = try {
            igdbService.search(condition)
        } catch (e: Exception) {
            throw ServiceException("502-1", "외부 게임 검색 서버 오류")
        } ?: emptyList() // 검색 결과가 null일 경우 빈 리스트 처리

        // 2. 장르 매핑 (Stream 대신 코틀린 컬렉션 함수 사용)
        val genreIgdbIds = igdbGames
            .flatMap { it.genres ?: emptyList() }
            .toSet()

        val genreMap = genreRepository.findByIgdbIdIn(genreIgdbIds.toMutableList())
            .filter { it.igdbId != null } // igdbId가 null이 아닌 것만
            .associate { it.igdbId!! to (it.name ?: "") }

        // 3. 플랫폼 수집
        val platformIds = igdbGames
            .flatMap { it.platforms ?: emptyList() }
            .toSet()

        val platformMap = igdbService.getPlatformNameMap(platformIds)

        // 4. DTO 변환
        return igdbGames.map { dto ->
            GameSearchResponse.fromDto(
                dto,
                genreMap,
                platformMap
            )
        }
    }
}