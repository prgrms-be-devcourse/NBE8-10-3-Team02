package com.back.domain.member.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AuthSignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 4, max = 50, message = "비밀번호는 4~50자여야 합니다.")
    val password: String,
    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 30, message = "닉네임은 2~30자여야 합니다.")
    val nickname: String,
)
