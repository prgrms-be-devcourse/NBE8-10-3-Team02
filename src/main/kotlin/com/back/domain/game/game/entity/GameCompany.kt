package com.back.domain.game.game.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    name = "game_company",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_company_game_company_role",
            columnNames = ["game_id", "company_id", "role"],
        ),
    ],
)
class GameCompany(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    var game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: CompanyRole,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_company_seq")
    @SequenceGenerator(name = "game_company_seq", sequenceName = "game_company_id_seq", allocationSize = 50)
    var id: Long? = null
        protected set

    companion object {
        @JvmStatic
        fun createGameCompany(
            game: Game,
            company: Company,
            role: CompanyRole,
        ) = GameCompany(game, company, role)
    }
}
