package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "game_theme",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_theme_game_theme",
            columnNames = ["game_id", "theme_id"],
        ),
    ],
)
class GameTheme(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    var theme: Theme,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_theme_seq")
    @SequenceGenerator(name = "game_theme_seq", sequenceName = "game_theme_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        fun createGameTheme(
            game: Game,
            theme: Theme,
        ) = GameTheme(game, theme)
    }
}
