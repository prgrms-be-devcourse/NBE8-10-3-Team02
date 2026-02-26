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
// (gameId, platformId) 유니크 제약조건
// db data가 stale 되었을 때 (snapshot찍은지 오래되면 stale) 갱신 로직에서 두번 들어갈 수 있다.
@Table(
    name = "game_platform",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_platform_game_platform",
            columnNames = ["game_id", "platform_id"],
        ),
    ],
)
class GamePlatform(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    var platform: Platform,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_platform_seq")
    @SequenceGenerator(name = "game_platform_seq", sequenceName = "game_platform_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        @JvmStatic
        fun createGamePlatform(
            game: Game,
            platform: Platform,
        ) = GamePlatform(game, platform)
    }
}
