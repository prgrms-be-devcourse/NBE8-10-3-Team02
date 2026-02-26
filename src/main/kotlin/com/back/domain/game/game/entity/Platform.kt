package com.back.domain.game.game.entity

import jakarta.persistence.*

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
