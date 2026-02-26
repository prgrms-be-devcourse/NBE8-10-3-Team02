package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "game_player_perspective",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_player_perspective_game_pp",
            columnNames = ["game_id", "player_perspective_id"],
        ),
    ],
)
class GamePlayerPerspective(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_perspective_id", nullable = false)
    var playerPerspective: PlayerPerspective,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_player_perspective_seq")
    @SequenceGenerator(name = "game_player_perspective_seq", sequenceName = "game_player_perspective_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        fun createGamePlayerPerspective(
            game: Game,
            playerPerspective: PlayerPerspective,
        ) = GamePlayerPerspective(game, playerPerspective)
    }
}
