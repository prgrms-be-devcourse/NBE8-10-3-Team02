package com.back.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class SecurityUser extends User {
    private final int id;

    @Getter
    private final String email;

    @Getter
    private final String nickname;

    public SecurityUser(
            int id,
            String email,
            String nickname,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(email, password, authorities);
        this.id = id;
        this.email = email;
        this.nickname = nickname;
    }

    public int getId() {
        return this.id;
    }
}