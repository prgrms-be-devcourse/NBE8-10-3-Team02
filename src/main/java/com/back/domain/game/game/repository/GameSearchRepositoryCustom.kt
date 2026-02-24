package com.back.domain.game.game.repository

import com.back.domain.game.game.dto.GameSearchCondition
import com.back.domain.game.game.entity.Game

interface GameSearchRepositoryCustom {
    fun searchByCondition(condition: GameSearchCondition): List<Game>
}