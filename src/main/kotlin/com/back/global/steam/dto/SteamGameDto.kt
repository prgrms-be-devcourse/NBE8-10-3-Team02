package com.back.global.steam.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SteamGameDto(
    @JsonProperty("appid") val appId: Long,
    val name: String?,
    @JsonProperty("playtime_forever") val playtimeForever: Int,
    @JsonProperty("img_icon_url") val imgIconUrl: String?,
)
