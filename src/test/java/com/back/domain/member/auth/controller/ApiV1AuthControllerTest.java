package com.back.domain.member.auth.controller;

import com.back.domain.game.game.service.GenreSyncService;
import com.back.domain.member.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired MemberRepository memberRepository;

    private final ObjectMapper om = new ObjectMapper();

    @MockitoBean
    GenreSyncService genreSyncService;

    // 유니크 값 생성 유틸
    private String uniqueEmail(String prefix) {
        return prefix + "_" + System.nanoTime() + "@test.com";
    }

    private String uniqueNickname(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    @Test
    @DisplayName("이메일 중복 체크: 가입 전이면 available=true (유니크 이메일)")
    void checkEmail_available_true() throws Exception {
        String email = uniqueEmail("newuser");

        mvc.perform(get("/api/v1/auth/check-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("회원가입 성공: DB에 회원이 저장된다 (유니크 이메일/닉네임)")
    void signup_success() throws Exception {
        String email = uniqueEmail("user1");
        String nickname = uniqueNickname("유저1");

        var body = Map.of(
                "email", email,
                "password", "1234",
                "nickname", nickname
        );

        mvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"));

        assertThat(memberRepository.findByEmail(email)).isNotNull();
    }

    @Test
    @DisplayName("로그인 성공: Set-Cookie에 apiKey/accessToken이 내려온다 (유니크 이메일/닉네임)")
    void login_sets_cookies() throws Exception {
        String email = uniqueEmail("user2");
        String nickname = uniqueNickname("유저2");
        String password = "1234";

        // 1) 먼저 가입
        mvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email,
                                "password", password,
                                "nickname", nickname
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"));

        // 2) 로그인
        var result = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        String all = String.join(";", setCookies);

        assertThat(all).contains("apiKey=");
        assertThat(all).contains("accessToken=");
    }

    @Test
    @DisplayName("로그아웃: 쿠키 삭제(Set-Cookie Max-Age=0) 내려온다")
    void logout_deletes_cookies() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        String all = String.join(";", setCookies);

        assertThat(all).contains("Max-Age=0");
    }
}
