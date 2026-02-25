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
    fun syncGenres() {
        val igdbGenres = igdbService.getGenres()

        val igdbIds = igdbGenres.map { it.id }

        val existingIds =
            genreRepository
                .findByIgdbIdIn(igdbIds)
                .map { it.igdbId }
                .toSet()

        val newGenres =
            igdbGenres
                .filter { dto -> !existingIds.contains(dto.id) }
                .map { dto -> Genre(igdbId = dto.id, name = dto.name) }

        if (newGenres.isNotEmpty()) {
            genreRepository.saveAll(newGenres)
        }
    }
}
