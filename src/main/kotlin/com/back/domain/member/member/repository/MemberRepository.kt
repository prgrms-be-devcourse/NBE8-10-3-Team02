package com.back.domain.member.member.repository

import com.back.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Int> {
    fun existsByEmail(email: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    fun existsByNicknameAndIdNot(
        nickname: String,
        id: Int,
    ): Boolean

    fun findByEmail(email: String): Member?

    fun findByApiKey(apiKey: String): Member?
}
