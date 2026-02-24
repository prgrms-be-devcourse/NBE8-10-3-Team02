package com.back.domain.member.auth.dto

import com.back.domain.member.member.entity.Member

data class AuthSignupResponse(
    val memberId: Int,
    val email: String?,
    val nickname: String?,
) {
    constructor(member: Member) : this(
        memberId = member.id,
        email = member.email,
        nickname = member.nickname,
    )
}
