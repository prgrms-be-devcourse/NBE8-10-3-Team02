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
    name = "platform",
    uniqueConstraints = [UniqueConstraint(name = "uk_platform_igdb_id", columnNames = ["igdb_id"])],
)
class Platform(
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
        @JvmStatic
        fun createPlatform(
            igdbId: Long,
            name: String,
        ) = Platform(igdbId, name)
    }
}
