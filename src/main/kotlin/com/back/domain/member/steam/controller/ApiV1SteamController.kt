package com.back.domain.member.steam.controller

import com.back.domain.member.memberGame.importgame.dto.ImportMatchRequest
import com.back.domain.member.memberGame.importgame.service.ImportService
import com.back.domain.member.steam.service.SteamAuthService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import com.back.global.steam.SteamCircuitBreakerClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/steam")
@Tag(name = "Steam", description = "Steam 연동")
class ApiV1SteamController(
    private val steamAuthService: SteamAuthService,
    private val steamClient: SteamCircuitBreakerClient,
    private val importService: ImportService,
    private val rq: Rq,
) {
    @GetMapping("/auth/url")
    @Operation(summary = "Steam OpenID 로그인 URL 반환")
    fun getAuthUrl(): RsData<Map<String, String>> {
        val url = steamAuthService.buildAuthUrl()
        return RsData("200-1", "Steam 인증 URL", mapOf("url" to url))
    }

    @GetMapping("/auth/callback")
    @Operation(summary = "Steam OpenID 콜백 처리")
    fun handleCallback(
        @RequestParam params: Map<String, String>,
        response: HttpServletResponse,
    ) {
        try {
            val steamId = steamAuthService.validateAndExtractSteamId(params)
            response.sendRedirect("http://localhost:3000/library/import/steam?steamId=$steamId")
        } catch (e: Exception) {
            response.sendRedirect("http://localhost:3000/library/import/steam?error=auth_failed")
        }
    }

    @PostMapping("/members/{memberId}/library/import/steam")
    @Operation(summary = "Steam 보유 게임 가져오기")
    fun importSteamGames(
        @PathVariable memberId: Int,
        @RequestBody body: Map<String, String>,
    ): RsData<Map<String, String>> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        if (memberId != actor.id) throw ServiceException("403", "Cannot import to this library")

        val steamId = body["steamId"]?.takeIf { it.isNotBlank() }
            ?: throw ServiceException("400", "Steam ID is required")

        val ownedGames = steamClient.getOwnedGames(steamId)
        if (ownedGames.isEmpty()) {
            throw ServiceException("404", "Steam 라이브러리가 비어있거나 비공개입니다.")
        }

        val gameNames = ownedGames.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }
        val request = ImportMatchRequest(gameNames, "PC")
        val jobId = importService.matchGamesAsync(memberId, request)

        return RsData("200-1", "Steam 게임 매칭이 시작되었습니다.", mapOf("jobId" to jobId))
    }
}
