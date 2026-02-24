package com.back.domain.member.auth.dto

import com.back.domain.member.member.entity.Member

data class AuthLoginResponse(
    val memberId: Int,
    val email: String?,
    val nickname: String?,
    val apiKey: String?,
    val accessToken: String,
) {
    // Member 엔티티와 토큰 정보를 받아 생성하는 보조 생성자
    constructor(member: Member, apiKey: String?, accessToken: String) : this(
        memberId = member.id,
        email = member.email,
        nickname = member.nickname,
        apiKey = apiKey,
        accessToken = accessToken,
    )
}
