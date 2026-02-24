package com.back.domain.game.game.entity

import jakarta.persistence.*

/**
 * 게임과 장르의 다대다 관계를 해소하기 위한 중간 엔티티입니다.
 * (gameId, genreId) 유니크 제약조건을 통해 중복 등록을 방지합니다.
 */
@Entity
@Table(
    name = "game_genre",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_genre_game_genre",
            columnNames = ["game_id", "genre_id"]
        )
    ]
)
class GameGenre(
    // 1. 코틀린은 주 생성자에서 필드를 바로 선언합니다. (롬복 @Getter/Setter 대체)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    var genre: Genre

) {
    // 2. ID는 관례적으로 클래스 몸체에 선언하며, DB에서 생성되므로 초기값 0을 줍니다.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_genre_seq")
    @SequenceGenerator(
        name = "game_genre_seq",
        sequenceName = "game_genre_id_seq",
        allocationSize = 50
    )
    val id: Long = 0

    // 3. 정적 팩토리 메서드 (자바의 static 메서드 대체)
    companion object {
        /**
         * @JvmStatic을 붙여주면 자바 코드에서도 GameGenre.createGameGenre(...)로
         * 기존처럼 호출 가능합니다.
         */
        @JvmStatic
        fun createGameGenre(game: Game, genre: Genre): GameGenre {
            return GameGenre(
                game = game,
                genre = genre
            )
        }
    }
}