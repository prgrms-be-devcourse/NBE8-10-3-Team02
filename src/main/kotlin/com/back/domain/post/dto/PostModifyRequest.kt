package com.back.domain.post.dto

import jakarta.persistence.Column
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostModifyRequest(
    @field:NotBlank
    @field:Size(max = 20)
    val title: String,

    @field:NotBlank
    @field:Column(columnDefinition = "TEXT")
    val content: String,

    val tags: List<String> = emptyList()
)