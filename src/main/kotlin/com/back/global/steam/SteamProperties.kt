package com.back.global.steam

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "steam")
data class SteamProperties(
    val baseUrl: String,
    val apiKey: String,
)
