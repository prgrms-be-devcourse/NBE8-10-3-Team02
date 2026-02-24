package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member

data class MemberMeResponse(
    val id: Int,
    val email: String?,
    val nickname: String?,
) {
    // Member 엔티티를 받아 DTO를 생성하는 보조 생성자
    constructor(member: Member) : this(
        id = member.id,
        email = member.email,
        nickname = member.nickname,
    )
}
