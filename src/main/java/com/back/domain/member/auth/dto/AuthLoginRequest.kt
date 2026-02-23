package com.back.domain.member.auth.dto

import jakarta.validation.constraints.NotBlank

data class AuthLoginRequest(
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)