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
    name = "genre",
    uniqueConstraints = [UniqueConstraint(name = "uk_genre_igdb_id", columnNames = ["igdb_id"])],
)
class Genre(
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
        fun createGenre(
            igdbId: Long,
            name: String,
        ) = Genre(igdbId, name)
    }
}
