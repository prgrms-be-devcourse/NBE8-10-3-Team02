package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "company",
    uniqueConstraints = [UniqueConstraint(name = "uk_company_igdb_id", columnNames = ["igdb_id"])],
)
class Company(
    @Column(name = "igdb_id", nullable = false)
    var igdbId: Long,
    @Column(nullable = false)
    var name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_seq")
    @SequenceGenerator(name = "company_seq", sequenceName = "company_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        @JvmStatic
        fun createCompany(
            igdbId: Long,
            name: String,
        ) = Company(igdbId, name)
    }
}
