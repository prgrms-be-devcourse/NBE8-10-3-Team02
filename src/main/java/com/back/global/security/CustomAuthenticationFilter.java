package com.back.global.security;

import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.standard.util.Ut;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    private final MemberService memberService;
    private final Rq rq;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) return true;
        if (uri.startsWith("/api/v1/auth/")) return true;
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")) return true;
        if (uri.startsWith("/h2-console")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            work(); // 인증 시도 (실패해도 예외를 던지지 않음)
            filterChain.doFilter(request, response);
        } catch (ServiceException e) {
            RsData<Void> rsData = e.getRsData();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(rsData.statusCode());
            response.getWriter().write(Ut.json.toString(rsData));
        }
    }

    private void work() {
        String apiKey = "";
        String accessToken = "";

        String authorization = rq.getHeader("Authorization", "");

        if (!authorization.isBlank()) {
            if (!authorization.startsWith("Bearer ")) {
                return; // 형식이 틀리면 그냥 중단 (게스트 취급)
            }

            String[] bits = authorization.split(" ", 3);
            if (bits.length == 2) {
                accessToken = bits[1].trim();
            } else if (bits.length == 3) {
                apiKey = bits[1].trim();
                accessToken = bits[2].trim();
            }
        } else {
            apiKey = rq.getCookieValue("apiKey", "");
            accessToken = rq.getCookieValue("accessToken", "");
        }

        if (apiKey.isBlank() && accessToken.isBlank()) return;

        // 1. accessToken 우선 검증
        if (!accessToken.isBlank()) {
            Map<String, Object> payload = memberService.payload(accessToken);

            if (payload != null) {
                int id = ((Number) payload.get("id")).intValue();
                String email = (String) payload.get("email");
                String nickname = (String) payload.get("nickname");

                setAuthentication(id, email, nickname);
                return;
            }
        }

        // 2. accessToken이 실패했거나 없을 때 apiKey로 시도
        if (!apiKey.isBlank()) {
            Optional.ofNullable(memberService.findByApiKey(apiKey)).ifPresent(member -> {
                setAuthentication(member.getId(), member.getEmail(), member.getNickname());

                // 새 accessToken 발급 + 쿠키 갱신
                String newAccessToken = memberService.genAccessToken(member);
                rq.setCookie("accessToken", newAccessToken);
            });
        }

        // 여기까지 왔는데 인증이 안 된 거면 그냥 비로그인 상태로 다음 필터(SecurityConfig)로 넘어감
    }

    private void setAuthentication(int id, String email, String nickname) {
        UserDetails user = new SecurityUser(
                id,
                email,
                nickname,
                "",
                java.util.List.of()
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        user.getPassword(),
                        user.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
