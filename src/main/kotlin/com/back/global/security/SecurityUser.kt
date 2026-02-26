package com.back.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class SecurityUser(
    val id: Int,
    val email: String,
    val nickname: String,
    password: String,
    authorities: Collection<GrantedAuthority>,
) : User(email, password, authorities)
