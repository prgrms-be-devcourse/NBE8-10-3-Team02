package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
// (gameId, genreId) 유니크 제약조건
// db data가 stale 되었을 때 (snapshot찍은지 오래되면 stale) 갱신 로직에서 두번 들어갈 수 있다.
@Table(
    name = "game_genre",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_genre_game_genre",
            columnNames = ["game_id", "genre_id"],
        ),
    ],
)
class GameGenre(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    var genre: Genre,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_genre_seq")
    @SequenceGenerator(name = "game_genre_seq", sequenceName = "game_genre_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        @JvmStatic
        fun createGameGenre(
            game: Game,
            genre: Genre,
        ) = GameGenre(game, genre)
    }
}
