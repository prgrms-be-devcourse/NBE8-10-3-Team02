package com.back.global.igdb.dto

data class PopularityLists(
    val visits: List<PopularityPrimitiveRow>?,
    val want: List<PopularityPrimitiveRow>?,
    val twitch: List<PopularityPrimitiveRow>?,
)
