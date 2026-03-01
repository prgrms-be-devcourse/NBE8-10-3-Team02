package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@ConditionalOnWebApplication
class CustomAuthenticationFilter(
    private val memberService: MemberService,
    private val rq: Rq,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return when {
            !uri.startsWith("/api/") -> true
            uri.startsWith("/api/v1/auth/") -> true
            uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui") -> true
            uri.startsWith("/h2-console") -> true
            else -> false
        }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            work()
            filterChain.doFilter(request, response)
        } catch (e: ServiceException) {
            val rsData = e.rsData
            response.contentType = "application/json;charset=UTF-8"
            response.status = rsData.statusCode
            response.writer.write(Ut.Json.toString(rsData))
        }
    }

    private fun work() {
        // 1. 헤더나 쿠키에서 인증 데이터(토큰, API 키)를 먼저 추출합니다.
        val (accessToken, apiKey) = extractAuthData()

        if (accessToken.isBlank() && apiKey.isBlank()) return

        // 2. AccessToken으로 먼저 인증을 시도하고, 실패하거나 없을 경우 ApiKey로 시도합니다.
        if (authenticateByAccessToken(accessToken)) return
        authenticateByApiKey(apiKey)
    }

    // [분리] 토큰 및 API 키 추출 로직
    private fun extractAuthData(): Pair<String, String> {
        val authorization = rq.getHeader("Authorization", "")

        if (authorization.isNotBlank() && authorization.startsWith("Bearer ")) {
            val bits = authorization.split(" ")
            return when (bits.size) {
                2 -> bits[1].trim() to ""
                3 -> bits[2].trim() to bits[1].trim()
                else -> "" to ""
            }
        }

        return rq.getCookieValue("accessToken", "") to rq.getCookieValue("apiKey", "")
    }

    // [분리] AccessToken 기반 인증 처리
    private fun authenticateByAccessToken(accessToken: String): Boolean {
        if (accessToken.isBlank()) return false

        memberService.payload(accessToken)?.let { payload ->
            val id = (payload["id"] as? Number)?.toInt() ?: 0
            val email = payload["email"] as? String ?: ""
            val nickname = payload["nickname"] as? String ?: ""

            setAuthentication(id, email, nickname)
            return true
        }
        return false
    }

    // [분리] ApiKey 기반 인증 처리
    private fun authenticateByApiKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) return false

        memberService.findByApiKey(apiKey)?.let { member ->
            setAuthentication(
                member.id,
                member.email ?: "",
                member.nickname ?: "",
            )

            // 새 토큰 발급 및 쿠키 갱신
            val newAccessToken = memberService.genAccessToken(member)
            rq.setCookie("accessToken", newAccessToken)
            return true
        }
        return false
    }

    private fun setAuthentication(
        id: Int,
        email: String,
        nickname: String,
    ) {
        val user = SecurityUser(id, email, nickname, "", listOf())

        val authentication =
            UsernamePasswordAuthenticationToken(
                user,
                user.password,
                user.authorities,
            )

        SecurityContextHolder.getContext().authentication = authentication
    }
}
