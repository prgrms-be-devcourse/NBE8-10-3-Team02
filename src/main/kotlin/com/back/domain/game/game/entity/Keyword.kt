package com.back.domain.game.game.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "keyword",
    uniqueConstraints = [UniqueConstraint(name = "uk_keyword_igdb_id", columnNames = ["igdb_id"])],
)
class Keyword(
    @Column(name = "igdb_id", nullable = false)
    var igdbId: Long,
    @Column(nullable = false)
    var name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "keyword_seq")
    @SequenceGenerator(name = "keyword_seq", sequenceName = "keyword_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        fun createKeyword(
            igdbId: Long,
            name: String,
        ) = Keyword(igdbId, name)
    }
}
