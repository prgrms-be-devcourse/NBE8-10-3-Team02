package com.back.domain.game.gameLike.entity

import com.back.domain.game.game.entity.Game
import com.back.domain.member.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "game_like",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_like_member_game",
            columnNames = ["member_id", "game_id"],
        ),
    ],
    indexes = [
        Index(name = "ix_game_like_game_id", columnList = "game_id"),
        Index(name = "ix_game_like_member_id", columnList = "member_id"),
    ],
)
class GameLike protected constructor() : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    lateinit var member: Member

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    lateinit var game: Game

    @CreatedDate
    @Column(updatable = false)
    var createdAt: LocalDateTime? = null

    companion object {
        @JvmStatic
        fun createGameLike(
            member: Member,
            game: Game,
        ) = GameLike().apply {
            this.member = member
            this.game = game
            this.createdAt = LocalDateTime.now()
        }
    }
}
