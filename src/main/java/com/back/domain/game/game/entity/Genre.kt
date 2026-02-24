package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "genre",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_genre_igdb_id", columnNames = ["igdb_id"])
    ]
)
class Genre(
    // 1. 주요 필드를 주 생성자에 선언 (자바의 생성자 + 롬복 @Getter 대체)
    @Column(name = "igdb_id", nullable = false)
    var igdbId: Long,

    @Column(nullable = false)
    var name: String

) {
    // 2. ID는 DB Identity 전략이므로 초기값 0으로 선언
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    // 3. 기존 자바 코드와의 호환성을 위한 정적 메서드
    companion object {
        @JvmStatic
        fun createGenre(igdbId: Long, name: String): Genre {
            return Genre(
                igdbId = igdbId,
                name = name
            )
        }
    }
}