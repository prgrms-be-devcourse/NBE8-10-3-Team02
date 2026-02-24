package com.back.domain.post.postComment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostCommentCreateRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val content: String,

    val parentId: Int? = null
)