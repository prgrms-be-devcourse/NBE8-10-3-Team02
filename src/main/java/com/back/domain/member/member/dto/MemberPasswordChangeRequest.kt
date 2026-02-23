package com.back.domain.member.member.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MemberPasswordChangeRequest(
        @field:NotBlank(message = "현재 비밀번호는 필수입니다.")
        val oldPassword: String,

        @field:NotBlank(message = "새 비밀번호는 필수입니다.")
        @field:Size(min = 4, max = 50, message = "비밀번호는 4~50자여야 합니다.")
        val newPassword: String
)