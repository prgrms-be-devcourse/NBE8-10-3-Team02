package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Game
import org.springframework.data.jpa.repository.JpaRepository

interface GameSearchRepository :
    JpaRepository<Game, Long>,
    GameSearchRepositoryCustom
