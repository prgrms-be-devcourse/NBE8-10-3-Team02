package com.back.domain.member.member.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MemberNicknameChangeRequest(
        @field:NotBlank(message = "닉네임은 필수입니다.")
        @field:Size(min = 2, max = 30, message = "닉네임은 2~30자여야 합니다.")
        val nickname: String
)