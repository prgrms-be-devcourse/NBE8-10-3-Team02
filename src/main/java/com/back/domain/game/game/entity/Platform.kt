package com.back.domain.game.game.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "platform",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_platform_igdb_id", columnNames = ["igdb_id"])
    ]
)
class Platform(
    // 1. 주요 필드를 주 생성자에 선언.
    // 코틀린은 기본적으로 public이며, 내부적으로 getter/setter를 생성합니다.
    @Column(name = "igdb_id", nullable = false)
    var igdbId: Long,

    @Column(nullable = false)
    var name: String

) {
    // 2. ID는 DB Identity 전략.
    // 코틀린의 Long은 자바의 long/Long을 모두 포괄하며 상황에 맞게 컴파일됩니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    // 3. 기존 자바 코드(createPlatform)와의 하위 호환성을 위한 companion object
    companion object {
        @JvmStatic
        fun createPlatform(igdbId: Long, name: String): Platform {
            return Platform(
                igdbId = igdbId,
                name = name
            )
        }
    }
}