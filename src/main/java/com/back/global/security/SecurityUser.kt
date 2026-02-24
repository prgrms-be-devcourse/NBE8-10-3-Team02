package com.back.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class SecurityUser(
    val id: Int,       // val을 쓰면 코틀린이 getter를 자동으로 만듭니다.
    val email: String,
    val nickname: String,
    password: String,
    authorities: Collection<out GrantedAuthority>
) : User(email, password, authorities)