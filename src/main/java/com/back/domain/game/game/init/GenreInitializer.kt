package com.back.domain.game.game.init

import com.back.domain.game.game.service.GenreSyncService
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
// 1. 생성자 주입: 클래스 선언부에서 바로 주입받습니다.
class GenreInitializer(
    private val genreSyncService: GenreSyncService
) {

    @PostConstruct
    fun init() {
        genreSyncService.syncGenres()
    }
}