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
    uniqueConstraints = [
        UniqueConstraint(name = "uk_genre_igdb_id", columnNames = ["igdb_id"]),
    ],
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

    companion object {
        // 자바 테스트 코드의 Genre.builder()를 받아주기 위한 정적 메서드
        @JvmStatic
        fun builder(): GenreBuilder = GenreBuilder()

        @JvmStatic
        fun createGenre(
            igdbId: Long,
            name: String,
        ): Genre =
            Genre(
                igdbId = igdbId,
                name = name,
            )
    }

    // 자바 팀원들의 기존 빌더 패턴 코드를 수용하기 위한 중첩 클래스
    class GenreBuilder {
        private var id: Long? = null
        private var igdbId: Long = 0
        private var name: String = ""

        fun id(id: Long?) = apply { this.id = id }

        fun igdbId(igdbId: Long) = apply { this.igdbId = igdbId }

        fun name(name: String) = apply { this.name = name }

        fun build(): Genre {
            val genre =
                Genre(
                    igdbId = igdbId,
                    name = name,
                )
            genre.id = this.id
            return genre
        }
    }
}
