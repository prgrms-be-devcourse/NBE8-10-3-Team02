package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "theme",
    uniqueConstraints = [UniqueConstraint(name = "uk_theme_igdb_id", columnNames = ["igdb_id"])],
)
class Theme(
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
        fun createTheme(
            igdbId: Long,
            name: String,
        ) = Theme(igdbId, name)
    }
}
