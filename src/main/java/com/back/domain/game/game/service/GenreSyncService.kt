package com.back.domain.game.game.service

import com.back.domain.game.game.entity.Genre
import com.back.domain.game.game.repository.GenreRepository
import com.back.global.igdb.service.IgdbService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GenreSyncService(
    private val igdbService: IgdbService,
    private val genreRepository: GenreRepository
) {
    /**
     * 서버 구동 시 IGDB의 장르 데이터를 DB와 동기화합니다.
     * 기존에 존재하지 않는 장르만 선별하여 저장합니다.
     */
    fun syncGenres() {
        // 1. IGDB로부터 모든 장르 가져오기 (!! 제거)
        val igdbGenres = igdbService.getGenres()

        // 2. IGDB ID 목록 추출 (코틀린 map 활용)
        val igdbIds = igdbGenres.map { it.id }

        // 3. DB에 이미 존재하는 장르의 IGDB ID 세트 만들기
        val existingIds = genreRepository.findByIgdbIdIn(igdbIds)
            .map { it.igdbId }
            .toSet()

        // 4. 존재하지 않는 장르만 필터링하여 엔티티로 변환 후 저장
        val newGenres = igdbGenres
            .filter { dto -> !existingIds.contains(dto.id) }
            .map { dto -> Genre(igdbId = dto.id, name = dto.name) }

        if (newGenres.isNotEmpty()) {
            genreRepository.saveAll(newGenres)
        }
    }
}