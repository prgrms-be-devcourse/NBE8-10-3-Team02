package com.back.domain.game.game.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "game_mode",
    uniqueConstraints = [UniqueConstraint(name = "uk_game_mode_igdb_id", columnNames = ["igdb_id"])],
)
class GameMode(
    @Column(name = "igdb_id", nullable = false)
    var igdbId: Long,
    @Column(nullable = false)
    var name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun createGameMode(
            igdbId: Long,
            name: String,
        ) = GameMode(igdbId, name)
    }
}
