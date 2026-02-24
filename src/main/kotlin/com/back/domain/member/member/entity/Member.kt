package com.back.domain.member.member.entity

import com.back.domain.game.game.entity.Game
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.entity.MemberGame
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
class Member(
    @Column(unique = true)
    var email: String? = null,
    var password: String? = null,
    @Column(unique = true, length = 30, nullable = false)
    var nickname: String? = null,
) : BaseEntity() {
    @Column(unique = true)
    var apiKey: String = UUID.randomUUID().toString()

    @CreatedDate
    @Column(updatable = false)
    var createDate: LocalDateTime? = null

    @LastModifiedDate
    var modifyDate: LocalDateTime? = null

    @OneToMany(
        mappedBy = "member",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.REMOVE],
        orphanRemoval = true,
    )
    val library: MutableList<MemberGame> = mutableListOf()

    // 보조 생성자: 특정 ID가 필요한 경우 (자바의 public Member(int id, ...) 대응)
    constructor(id: Int, email: String, nickname: String) : this(email, null, nickname) {
        // 하위 클래스에서 BaseEntity의 id에 접근하여 값 세팅
        // 이를 위해 BaseEntity의 id는 var여야 합니다.
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(this, id)
    }

    fun changePassword(encodedPassword: String) {
        this.password = encodedPassword
    }

    fun changeNickname(nickname: String) {
        this.nickname = nickname
    }

    fun addMemberGame(
        platformId: Long,
        playtime: Double,
        isFavorite: Boolean,
        status: StatusEnum,
        game: Game,
    ): MemberGame {
        val memberGame = MemberGame(platformId, playtime, isFavorite, status, this, game)
        library.add(memberGame)
        return memberGame
    }

    // 자바의 Optional<MemberGame> 대신 코틀린의 Nullable(?)을 사용
    fun getMemberGameById(memberGameId: Int): MemberGame? = library.find { it.id == memberGameId }

    fun removeGame(memberGameId: Int): Boolean = library.removeIf { it.id == memberGameId }
}
