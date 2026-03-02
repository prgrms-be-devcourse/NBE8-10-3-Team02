package com.back.global.steam

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "steam")
data class SteamProperties(
    val baseUrl: String,
    val apiKey: String,
    val openidRealm: String = "http://localhost:8080",
    val openidReturnTo: String = "http://localhost:8080/api/v1/steam/auth/callback",
)
