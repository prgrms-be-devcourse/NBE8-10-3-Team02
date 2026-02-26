package com.back.global.app

import com.back.standard.util.Ut
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AppConfig(
    private val objectMapper: ObjectMapper,
) {
    @Autowired
    fun setEnvironment(env: Environment) {
        environment = env
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @PostConstruct
    fun init() {
        Ut.Json.objectMapper = objectMapper
    }

    companion object {
        private var environment: Environment? = null

        @JvmStatic
        fun isDev(): Boolean = environment?.matchesProfiles("dev") ?: false

        @JvmStatic
        fun isTest(): Boolean = environment?.matchesProfiles("test") ?: false

        @JvmStatic
        fun isProd(): Boolean = environment?.matchesProfiles("prod") ?: false

        @JvmStatic
        fun isNotProd(): Boolean = !isProd()
    }
}
