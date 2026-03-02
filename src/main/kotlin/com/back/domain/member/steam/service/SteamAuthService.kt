package com.back.domain.member.steam.service

import com.back.global.steam.SteamProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

@Service
class SteamAuthService(
    private val steamProperties: SteamProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val STEAM_OPENID_URL = "https://steamcommunity.com/openid/login"
        private val STEAM_ID_PATTERN = Pattern.compile("https://steamcommunity\\.com/openid/id/(\\d+)")
    }

    fun buildAuthUrl(): String {
        val realm = steamProperties.openidRealm
        val returnTo = steamProperties.openidReturnTo
        return STEAM_OPENID_URL +
            "?openid.ns=" + enc("http://specs.openid.net/auth/2.0") +
            "&openid.mode=checkid_setup" +
            "&openid.return_to=" + enc(returnTo) +
            "&openid.realm=" + enc(realm) +
            "&openid.identity=" + enc("http://specs.openid.net/auth/2.0/identifier_select") +
            "&openid.claimed_id=" + enc("http://specs.openid.net/auth/2.0/identifier_select")
    }

    fun validateAndExtractSteamId(params: Map<String, String>): String {
        val mode = params["openid.mode"]
        require(mode == "id_res") { "Invalid OpenID mode: $mode" }

        val body = buildString {
            append("openid.ns=").append(enc(params["openid.ns"] ?: ""))
            append("&openid.mode=check_authentication")
            append("&openid.sig=").append(enc(params["openid.sig"] ?: ""))
            append("&openid.signed=").append(enc(params["openid.signed"] ?: ""))
            val signed = params["openid.signed"] ?: ""
            for (field in signed.split(",")) {
                val key = "openid.$field"
                append("&").append(enc(key)).append("=").append(enc(params[key] ?: ""))
            }
        }

        val client = RestClient.create()
        val response = client.post()
            .uri(STEAM_OPENID_URL)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(body)
            .retrieve()
            .body(String::class.java)

        require(response != null && response.contains("is_valid:true")) {
            "Steam OpenID validation failed"
        }

        val identity = params["openid.identity"]
            ?: throw IllegalArgumentException("Missing openid.identity")

        val matcher = STEAM_ID_PATTERN.matcher(identity)
        require(matcher.find()) { "Could not extract Steam ID from: $identity" }
        return matcher.group(1)
    }

    private fun enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8)
}
