package com.back.domain.game.game.entity

import jakarta.persistence.*


@Entity
@Table(
    name = "game_platform",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_platform_game_platform",
            columnNames = ["game_id", "platform_id"]
        )
    ]
)
class GamePlatform(
    // 1. 주 생성자에서 연관 관계 엔티티를 필수로 받도록 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    var platform: Platform

) {
    // 2. ID는 DB에서 자동 생성되므로 기본값 0(또는 null)으로 선언
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_platform_seq")
    @SequenceGenerator(
        name = "game_platform_seq",
        sequenceName = "game_platform_id_seq",
        allocationSize = 50
    )
    var id: Long?  = null
    protected set

    // 3. 기존 자바의 createGamePlatform 정적 메서드 유지 (하위 호환성 및 명시적 생성)
    companion object {
        @JvmStatic
        fun createGamePlatform(game: Game, platform: Platform): GamePlatform {
            // 코틀린 생성자를 호출하여 반환
            return GamePlatform(
                game = game,
                platform = platform
            )
        }
    }
}