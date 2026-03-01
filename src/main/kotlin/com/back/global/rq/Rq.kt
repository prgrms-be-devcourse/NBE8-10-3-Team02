package com.back.global.rq

import com.back.domain.member.member.entity.Member
import com.back.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope // 매 요청마다 새 객체를 생성하여 스레드 간 충돌 방지
@ConditionalOnWebApplication
class Rq(
    private val req: HttpServletRequest,
    private val resp: HttpServletResponse,
) {
    // fun getActor(): Member? 대신 'val actor' 프로퍼티 사용
    val actor: Member?
        get() {
            val principal = SecurityContextHolder.getContext().authentication?.principal ?: return null
            if (principal !is SecurityUser) return null

            return Member(
                id = principal.id,
                email = principal.email,
                nickname = principal.nickname,
            )
        }

    fun getHeader(
        name: String,
        defaultValue: String,
    ): String = req.getHeader(name)?.takeIf { it.isNotBlank() } ?: defaultValue

    fun setHeader(
        name: String,
        value: String?,
    ) {
        val safeValue = value ?: ""

        if (safeValue.isBlank()) {
            req.removeAttribute(name)
        } else {
            resp.setHeader(name, safeValue)
        }
    }

    fun getCookieValue(
        name: String,
        defaultValue: String,
    ): String {
        val cookies = req.cookies ?: return defaultValue

        return cookies
            .firstOrNull { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue
    }

    /**
     * 쿠키를 설정합니다.
     * apply 함수를 사용하여 가독성을 높였습니다.
     */
    fun setCookie(
        name: String,
        value: String?,
    ) {
        val safeValue = value ?: ""

        val cookie =
            Cookie(name, safeValue).apply {
                path = "/"
                isHttpOnly = true
                domain = "localhost"
                secure = true
                setAttribute("SameSite", "Strict")
                maxAge = if (safeValue.isBlank()) 0 else 60 * 60 * 24 * 365
            }

        resp.addCookie(cookie)
    }

    fun deleteCookie(name: String) {
        setCookie(name, null)
    }
}
