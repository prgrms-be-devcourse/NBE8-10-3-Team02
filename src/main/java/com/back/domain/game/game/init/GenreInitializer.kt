package com.back.domain.game.game.init

import com.back.domain.game.game.service.GenreSyncService
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class GenreInitializer(
    private val genreSyncService: GenreSyncService,
) {
    @PostConstruct
    fun init() {
        genreSyncService.syncGenres()
    }
}
