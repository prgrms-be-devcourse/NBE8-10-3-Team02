package com.back.domain.game.game.service

import com.back.domain.game.game.dto.GenreResponse
import com.back.domain.game.game.repository.GenreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GenreService(
    private val genreRepository: GenreRepository,
) {
    fun getGenres(): List<GenreResponse> =
        genreRepository.findAll().map { genre ->
            GenreResponse(
                id = genre.igdbId,
                name = genre.name,
            )
        }
}
