package com.back.domain.game.game.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "game_game_mode",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_game_mode_game_game_mode",
            columnNames = ["game_id", "game_mode_id"],
        ),
    ],
)
class GameGameMode(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_mode_id", nullable = false)
    var gameMode: GameMode,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_game_mode_seq")
    @SequenceGenerator(name = "game_game_mode_seq", sequenceName = "game_game_mode_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        fun createGameGameMode(
            game: Game,
            gameMode: GameMode,
        ) = GameGameMode(game, gameMode)
    }
}
