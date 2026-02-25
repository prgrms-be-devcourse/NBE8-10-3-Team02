package com.back.domain.member.memberGame.controller;

import com.back.domain.game.game.entity.Game;
import com.back.domain.game.game.repository.GameRepository;
import com.back.domain.game.game.service.GenreSyncService;
import com.back.domain.member.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1MemberGameControllerTest {

    @Autowired MockMvc mvc;
    @Autowired MemberRepository memberRepository;
    @Autowired GameRepository gameRepository;

    private final ObjectMapper om = new ObjectMapper();

    @MockitoBean
    GenreSyncService genreSyncService;

    // --- helpers ---

    private String uniqueEmail(String prefix) {
        return prefix + "_" + System.nanoTime() + "@test.com";
    }

    private String uniqueNickname(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    private void signup(String email, String password, String nickname) throws Exception {
        mvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email,
                                "password", password,
                                "nickname", nickname
                        ))))
                .andExpect(status().isCreated());
    }

    private Cookie[] loginAndGetCookies(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookies();
    }

    private Game createTestGame() {
        Game game = Game.createGame(
                System.nanoTime(),
                "Test Game " + System.nanoTime(),
                "Test Summary",
                null,
                LocalDate.now()
        );
        return gameRepository.save(game);
    }

    /** 라이브러리에 게임을 추가하고 MvcResult 반환 (memberGameId 파싱용) */
    private MvcResult addGameToLibrary(Cookie[] cookies, int memberId, int gameId) throws Exception {
        return mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC",
                                "playtime", 0.0,
                                "isFavorite", false,
                                "status", "PLAYING",
                                "gameId", gameId
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    // --- tests ---

    @Test
    @DisplayName("라이브러리 조회: 성공 (200-1)")
    void viewLibrary_success() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        mvc.perform(get("/api/v1/members/{memberId}/library", memberId)
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("라이브러리 조회: 비로그인 → 401")
    void viewLibrary_unauthorized() throws Exception {
        mvc.perform(get("/api/v1/members/{memberId}/library", 1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("라이브러리 조회: 다른 유저의 라이브러리 → 403")
    void viewLibrary_forbidden() throws Exception {
        String email1 = uniqueEmail("user1");
        signup(email1, "1234", uniqueNickname("user1"));
        Cookie[] cookies1 = loginAndGetCookies(email1, "1234");

        String email2 = uniqueEmail("user2");
        signup(email2, "1234", uniqueNickname("user2"));
        int memberId2 = memberRepository.findByEmail(email2).getId();

        mvc.perform(get("/api/v1/members/{memberId}/library", memberId2)
                        .cookie(cookies1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403"));
    }

    @Test
    @DisplayName("라이브러리 조회: status 필터 적용")
    void viewLibrary_withStatusFilter() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game1 = createTestGame();
        Game game2 = createTestGame();

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC", "playtime", 0.0, "isFavorite", false,
                                "status", "PLAYING", "gameId", game1.getId()))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC", "playtime", 0.0, "isFavorite", false,
                                "status", "COMPLETED", "gameId", game2.getId()))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/members/{memberId}/library", memberId)
                        .cookie(cookies)
                        .param("status", "PLAYING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PLAYING"));
    }

    @Test
    @DisplayName("라이브러리 조회: platform 필터 적용")
    void viewLibrary_withPlatformFilter() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game1 = createTestGame();
        Game game2 = createTestGame();

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC", "playtime", 0.0, "isFavorite", false,
                                "status", "PLAYING", "gameId", game1.getId()))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PS", "playtime", 0.0, "isFavorite", false,
                                "status", "PLAYING", "gameId", game2.getId()))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/members/{memberId}/library", memberId)
                        .cookie(cookies)
                        .param("platform", "PC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].platform").value("PC"));
    }

    @Test
    @DisplayName("라이브러리 게임 추가: 성공 (201-1)")
    void addToLibrary_success() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC",
                                "playtime", 10.5,
                                "isFavorite", true,
                                "status", "PLAYING",
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.gameId").value(game.getId()))
                .andExpect(jsonPath("$.data.platform").value("PC"))
                .andExpect(jsonPath("$.data.status").value("PLAYING"))
                .andExpect(jsonPath("$.data.isFavorite").value(false));
    }

    @Test
    @DisplayName("라이브러리 게임 추가: 비로그인 → 401")
    void addToLibrary_unauthorized() throws Exception {
        Game game = createTestGame();

        mvc.perform(post("/api/v1/members/{memberId}/library", 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC",
                                "playtime", 0.0,
                                "isFavorite", false,
                                "status", "PLAYING",
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("라이브러리 게임 추가: 다른 유저 라이브러리에 추가 → 403")
    void addToLibrary_forbidden() throws Exception {
        String email1 = uniqueEmail("user1");
        signup(email1, "1234", uniqueNickname("user1"));
        Cookie[] cookies1 = loginAndGetCookies(email1, "1234");

        String email2 = uniqueEmail("user2");
        signup(email2, "1234", uniqueNickname("user2"));
        int memberId2 = memberRepository.findByEmail(email2).getId();

        Game game = createTestGame();

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId2)
                        .with(csrf())
                        .cookie(cookies1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC",
                                "playtime", 0.0,
                                "isFavorite", false,
                                "status", "PLAYING",
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403"));
    }

    @Test
    @DisplayName("라이브러리 게임 추가: 중복 추가 → 400-2")
    void addToLibrary_duplicate() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        addGameToLibrary(cookies, memberId, game.getId());

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC",
                                "playtime", 0.0,
                                "isFavorite", false,
                                "status", "PLAYING",
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @DisplayName("라이브러리 게임 추가: 유효하지 않은 플랫폼 → 400-3")
    void addToLibrary_invalidPlatform() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "INVALID_PLATFORM",
                                "playtime", 0.0,
                                "isFavorite", false,
                                "status", "PLAYING",
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));
    }

    @Test
    @DisplayName("라이브러리 게임 추가: 없는 게임 → 404-1")
    void addToLibrary_gameNotFound() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "platform", "PC",
                                "playtime", 0.0,
                                "isFavorite", false,
                                "status", "PLAYING",
                                "gameId", 999999
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("라이브러리 게임 상태 업데이트: 성공 (200)")
    void updateMemberGame_success() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        MvcResult addResult = addGameToLibrary(cookies, memberId, game.getId());
        int memberGameId = om.readTree(addResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        mvc.perform(patch("/api/v1/members/{memberId}/library/{memberGameId}", memberId, memberGameId)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "status", "COMPLETED",
                                "playtime", 50,
                                "isFavorite", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.isFavorite").value(true));
    }

    @Test
    @DisplayName("라이브러리 게임 상태 업데이트: 비로그인 → 401")
    void updateMemberGame_unauthorized() throws Exception {
        mvc.perform(patch("/api/v1/members/{memberId}/library/{memberGameId}", 1, 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("라이브러리 게임 상태 업데이트: 다른 유저의 게임 → 403")
    void updateMemberGame_forbidden() throws Exception {
        String ownerEmail = uniqueEmail("owner");
        signup(ownerEmail, "1234", uniqueNickname("owner"));
        Cookie[] ownerCookies = loginAndGetCookies(ownerEmail, "1234");
        int ownerId = memberRepository.findByEmail(ownerEmail).getId();

        String otherEmail = uniqueEmail("other");
        signup(otherEmail, "1234", uniqueNickname("other"));
        Cookie[] otherCookies = loginAndGetCookies(otherEmail, "1234");
        int otherId = memberRepository.findByEmail(otherEmail).getId();

        Game game = createTestGame();
        MvcResult addResult = addGameToLibrary(ownerCookies, ownerId, game.getId());
        int memberGameId = om.readTree(addResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        // otherId passes URL check (own memberId), but memberGameId belongs to owner
        mvc.perform(patch("/api/v1/members/{memberId}/library/{memberGameId}", otherId, memberGameId)
                        .with(csrf())
                        .cookie(otherCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "DROPPED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("라이브러리에서 게임 삭제: 성공 (204)")
    void removeFromLibrary_success() throws Exception {
        String email = uniqueEmail("user");
        String nickname = uniqueNickname("user");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        MvcResult addResult = addGameToLibrary(cookies, memberId, game.getId());
        int memberGameId = om.readTree(addResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        mvc.perform(delete("/api/v1/members/{memberId}/library/{memberGameId}", memberId, memberGameId)
                        .with(csrf())
                        .cookie(cookies))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.resultCode").value("204"));
    }

    @Test
    @DisplayName("라이브러리에서 게임 삭제: 비로그인 → 401")
    void removeFromLibrary_unauthorized() throws Exception {
        mvc.perform(delete("/api/v1/members/{memberId}/library/{memberGameId}", 1, 1)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("라이브러리에서 게임 삭제: 다른 유저 라이브러리 → 403")
    void removeFromLibrary_forbidden() throws Exception {
        String ownerEmail = uniqueEmail("owner");
        signup(ownerEmail, "1234", uniqueNickname("owner"));
        Cookie[] ownerCookies = loginAndGetCookies(ownerEmail, "1234");
        int ownerId = memberRepository.findByEmail(ownerEmail).getId();

        String otherEmail = uniqueEmail("other");
        signup(otherEmail, "1234", uniqueNickname("other"));
        Cookie[] otherCookies = loginAndGetCookies(otherEmail, "1234");

        Game game = createTestGame();
        MvcResult addResult = addGameToLibrary(ownerCookies, ownerId, game.getId());
        int memberGameId = om.readTree(addResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        // Other user tries to delete using owner's memberGameId on owner's memberId path
        mvc.perform(delete("/api/v1/members/{memberId}/library/{memberGameId}", ownerId, memberGameId)
                        .with(csrf())
                        .cookie(otherCookies))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403"));
    }
}