package com.back.domain.game

import com.back.domain.game.game.entity.Game

/**
 * DB에서 조회된 것처럼 id가 세팅된 Game 객체를 만드는 헬퍼
 */
object GameTestFixtures {
    @JvmStatic
    @JvmOverloads
    fun gameWithId(
        id: Long,
        igdbId: Long = id * 10L,
        name: String = "Game$id",
        summary: String = "",
    ): Game {
        val game = Game.createGame(igdbId, name, summary, null, 1700000000L, null, null, null, null)
        Game::class.java
            .getDeclaredField("id") // private 필드 id 가져옴
            .also {
                it.isAccessible = true // private 접근 제한 해제
                it.set(game, id) // 값 주입
            }
        return game
    }
}
