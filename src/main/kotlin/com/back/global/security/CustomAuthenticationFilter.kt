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
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
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
        val authorization = rq.getHeader("Authorization", "")
        var accessToken = ""
        var apiKey = ""

        // --- [자바 로직 복구: 토큰/API 키 추출] ---
        if (authorization.isNotBlank()) {
            if (!authorization.startsWith("Bearer ")) return

            val bits = authorization.split(" ")
            if (bits.size == 2) {
                accessToken = bits[1].trim()
            } else if (bits.size == 3) {
                apiKey = bits[1].trim()
                accessToken = bits[2].trim()
            }
        } else {
            apiKey = rq.getCookieValue("apiKey", "")
            accessToken = rq.getCookieValue("accessToken", "")
        }
        // ------------------------------------------

        if (apiKey.isBlank() && accessToken.isBlank()) return

        // 1. accessToken 우선 검증
        if (accessToken.isNotBlank()) {
            memberService.payload(accessToken)?.let { payload ->
                // as 대신 as?와 엘비스 연산자를 조합하여 안전하게 값을 가져옵니다.
                val id = (payload["id"] as? Number)?.toInt() ?: 0
                val email = payload["email"] as? String ?: ""
                val nickname = payload["nickname"] as? String ?: ""

                setAuthentication(id, email, nickname)
                return
            }
        }

        // 2. apiKey로 시도
        if (apiKey.isNotBlank()) {
            memberService.findByApiKey(apiKey)?.let { member ->
                setAuthentication(
                    member.id,
                    member.email ?: "",
                    member.nickname ?: ""
                )

                // 새 accessToken 발급 + 쿠키 갱신
                val newAccessToken = memberService.genAccessToken(member)
                rq.setCookie("accessToken", newAccessToken)
            }
        }
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
