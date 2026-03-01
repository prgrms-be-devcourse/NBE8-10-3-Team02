package com.back.global.security

import com.back.global.rsData.RsData
import com.back.standard.util.Ut
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke // 핵심: http { } 문법 활성화
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() } // 상단에 org.apache.catalina... 임포트가 있으면 안 됨!
            cors { }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            formLogin { disable() }
            httpBasic { disable() }
            headers { frameOptions { sameOrigin = true } }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(customAuthenticationFilter)

            exceptionHandling {
                // 인터페이스 이름을 명시하여 람다 에러 해결
                authenticationEntryPoint =
                    AuthenticationEntryPoint { _, response, _ ->
                        response.contentType = "application/json;charset=UTF-8"
                        response.status = 401
                        response.writer.write(Ut.Json.toString(RsData<Void>("401-1", "로그인 후 이용해주세요.")))
                    }
                accessDeniedHandler =
                    AccessDeniedHandler { _, response, _ ->
                        response.contentType = "application/json;charset=UTF-8"
                        response.status = 403
                        response.writer.write(Ut.Json.toString(RsData<Void>("403-1", "권한이 없습니다.")))
                    }
            }

            authorizeHttpRequests {
                // 1. Preflight 요청 허용
                authorize(HttpMethod.OPTIONS, "/**", permitAll)

                // 2. Swagger / H2 Console 허용
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/h2-console/**", permitAll)

                // 3. 인증 관련 API 허용
                authorize("/api/v1/auth/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/members/check-nickname", permitAll)
                authorize(HttpMethod.GET, "/api/v1/auth/check-email", permitAll)

                // 4. 게임 관련 조회 허용
                authorize(HttpMethod.GET, "/api/v1/games/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/genres/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/platforms/**", permitAll)

                // 5. 게시글/댓글 조회 허용
                authorize(HttpMethod.GET, "/api/v1/posts/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/posts/*/comments/**", permitAll)

                // 6. 리뷰 조회 허용
                authorize(HttpMethod.GET, "/api/v1/reviews", permitAll)
                authorize(HttpMethod.GET, "/api/v1/reviews/game/**", permitAll)

                // 7. 그 외 모든 /api/** 요청은 로그인 필요
                authorize("/api/**", authenticated)

                // 8. 나머지는 모두 허용 (정적 리소스 등)
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }
}
