package com.back.domain.game.game.service

import com.back.domain.game.game.dto.GenreResponse
import com.back.domain.game.game.repository.GenreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GenreService(
    private val genreRepository: GenreRepository
) {

    /**
     * 모든 장르를 조회하여 DTO 리스트로 반환합니다.
     * 코틀린의 컬렉션 함수를 사용하여 자바 스트림보다 간결하게 처리합니다.
     */
    fun getGenres(): List<GenreResponse> {
        // genreRepository.findAll()은 List<Genre>를 반환합니다.
        return genreRepository.findAll().map { genre ->
            // GenreResponse의 생성자 파라미터 이름이 'id'인 것을 확인했습니다.
            // 엔티티의 'igdbId'를 DTO의 'id'에 매핑합니다.
            GenreResponse(
                id = genre.igdbId,
                name = genre.name
            )
        }
    }
}