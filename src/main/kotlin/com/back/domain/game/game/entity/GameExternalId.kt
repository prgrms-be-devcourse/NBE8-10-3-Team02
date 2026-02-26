package com.back.domain.game.game.entity

import jakarta.persistence.Column
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
    name = "game_external_id",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_external_id_game_platform_eid",
            columnNames = ["game_id", "platform", "external_id"],
        ),
    ],
)
class GameExternalId(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @Column(nullable = false)
    var platform: String,
    @Column(name = "external_id", nullable = false)
    var externalId: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_external_id_seq")
    @SequenceGenerator(name = "game_external_id_seq", sequenceName = "game_external_id_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        fun createGameExternalId(
            game: Game,
            platform: String,
            externalId: String,
        ) = GameExternalId(game, platform, externalId)
    }
}
