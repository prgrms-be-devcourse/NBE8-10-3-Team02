package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "game_keyword",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_keyword_game_keyword",
            columnNames = ["game_id", "keyword_id"],
        ),
    ],
)
class GameKeyword(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    var keyword: Keyword,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_keyword_seq")
    @SequenceGenerator(name = "game_keyword_seq", sequenceName = "game_keyword_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        fun createGameKeyword(
            game: Game,
            keyword: Keyword,
        ) = GameKeyword(game, keyword)
    }
}
