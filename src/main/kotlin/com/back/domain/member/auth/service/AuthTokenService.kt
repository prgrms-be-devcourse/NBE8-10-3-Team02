package com.back.domain.member.auth.service

import com.back.domain.member.member.entity.Member
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class AuthTokenService(
    @Value("\${custom.jwt.secretKey}")
    private val secretKeyString: String,
    @Value("\${custom.accessToken.expirationSeconds}")
    private val expirationSeconds: Int,
) {
    // SecretKey를 매번 생성하지 않도록 지연 초기화(lazy) 처리
    private val cachedKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKeyString.toByteArray())
    }

    /**
     * 액세스 토큰 생성
     */
    fun genAccessToken(member: Member): String {
        val now = Date()
        val exp = Date(now.time + 1000L * expirationSeconds)

        return Jwts
            .builder()
            .claim("id", member.id)
            .claim("email", member.email)
            .claim("nickname", member.nickname)
            .issuedAt(now)
            .expiration(exp)
            .signWith(cachedKey)
            .compact()
    }

    /**
     * 토큰에서 페이로드 추출
     */
    fun payload(token: String): Map<String, Any>? {
        return runCatching {
            Jwts
                .parser()
                .verifyWith(cachedKey)
                .build()
                .parse(token)
                .payload as Map<String, Any>
        }.getOrNull() // 실패 시(에러 발생 시) null 반환
    }
}
