package com.back.global.igdb

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "igdb")
data class IgdbProperties(
    val baseUrl: String,
    val clientId: String,
    val clientSecret: String,
    val tokenUrl: String,
)
