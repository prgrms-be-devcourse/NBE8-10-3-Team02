package com.back.global.security

import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@Primary // - MemberService 등에서 PasswordEncoder를 주입받을 때 최우선으로 선택됩니다.
class CachedPasswordEncoder(
    private val delegate: PasswordEncoder = BCryptPasswordEncoder()
) : PasswordEncoder {

    // - 400ms의 무거운 Bcrypt 연산을 캐싱합니다.
    // - key는 입력 비밀번호와 저장된 해시값을 조합합니다.
    @Cacheable(cacheNames = ["passwordMatches"], key = "#rawPassword.toString() + #encodedPassword")
    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean {
        return delegate.matches(rawPassword, encodedPassword)
    }

    override fun encode(rawPassword: CharSequence): String {
        return delegate.encode(rawPassword)
    }
}