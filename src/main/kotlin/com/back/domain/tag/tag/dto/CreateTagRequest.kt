package com.back.domain.tag.tag.dto

import jakarta.validation.constraints.NotBlank

data class CreateTagRequest(
    @field:NotBlank
    val content: String,
)
