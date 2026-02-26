package com.back.domain.review.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ReviewModifyRequest(
    @field:NotBlank val title: String,
    @field:NotBlank val content: String,
    @field:NotNull @field:DecimalMin("0") @field:DecimalMax("5") val rating: Double,
)