package com.back.global.igdb.dto

data class IgdbInvolvedCompanyDto(
    val id: Long,
    val company: IgdbCompanyDto?,
    val developer: Boolean?,
    val publisher: Boolean?,
)
