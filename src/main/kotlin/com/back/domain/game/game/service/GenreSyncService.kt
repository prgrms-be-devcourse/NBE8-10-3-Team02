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
    private val genreRepository: GenreRepository,
) {
    //    서버 켜질때 장르를 DB로 동기화
    fun syncGenres() {
        val igdbGenres = igdbService.getGenres()
        val existingIds =
            genreRepository
                .findByIgdbIdIn(igdbGenres.map { it.id })
                .map { it.igdbId }
                .toSet()

        for (dto in igdbGenres) {
            if (dto.id !in existingIds) {
                genreRepository.save(Genre(dto.id, dto.name ?: ""))
            }
        }
    }
}
