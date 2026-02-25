package com.back.domain.review.controller;

import com.back.domain.game.game.entity.Game;
import com.back.domain.game.game.repository.GameRepository;
import com.back.domain.game.game.service.GenreSyncService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.review.repository.ReviewRepository;
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
class ApiV1ReviewControllerTest {

    @Autowired MockMvc mvc;
    @Autowired MemberRepository memberRepository;
    @Autowired GameRepository gameRepository;
    @Autowired ReviewRepository reviewRepository;

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

    private void addGameToLibrary(Cookie[] cookies, int memberId, int gameId) throws Exception {
        mvc.perform(post("/api/v1/members/{memberId}/library", memberId)
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
                .andExpect(status().isCreated());
    }

    private MvcResult writeReview(Cookie[] cookies, int gameId, String title, String content, double rating) throws Exception {
        return mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", title,
                                "content", content,
                                "rating", rating,
                                "gameId", gameId
                        ))))
                .andReturn();
    }

    // --- tests ---

    @Test
    @DisplayName("리뷰 목록 조회: 200-1 반환")
    void getReviews_success() throws Exception {
        mvc.perform(get("/api/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("회원별 리뷰 목록 조회: 200-1 반환")
    void getReviewsByMember_success() throws Exception {
        String email = uniqueEmail("reviewer");
        String nickname = uniqueNickname("reviewer");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        mvc.perform(get("/api/v1/reviews/member/{memberId}", memberId)
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("게임별 리뷰 목록 조회: 200-1 반환")
    void getReviewsByGame_success() throws Exception {
        Game game = createTestGame();

        mvc.perform(get("/api/v1/reviews/game/{gameId}", game.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("리뷰 단건 조회: 성공")
    void getReview_success() throws Exception {
        String email = uniqueEmail("reviewer");
        String nickname = uniqueNickname("reviewer");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        addGameToLibrary(cookies, memberId, game.getId());

        MvcResult writeResult = writeReview(cookies, game.getId(), "Test Title", "Test Content", 4.5);
        int reviewId = om.readTree(writeResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        mvc.perform(get("/api/v1/reviews/{id}", reviewId)
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"))
                .andExpect(jsonPath("$.rating").value(4.5));
    }

    @Test
    @DisplayName("리뷰 단건 조회: 없는 ID → 404-1")
    void getReview_notFound() throws Exception {
        String email = uniqueEmail("reviewer");
        signup(email, "1234", uniqueNickname("reviewer"));
        Cookie[] cookies = loginAndGetCookies(email, "1234");

        mvc.perform(get("/api/v1/reviews/{id}", 999999)
                        .cookie(cookies))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("리뷰 작성: 성공 (라이브러리에 게임 있음)")
    void writeReview_success() throws Exception {
        String email = uniqueEmail("writer");
        String nickname = uniqueNickname("writer");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        addGameToLibrary(cookies, memberId, game.getId());

        mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Great Game",
                                "content", "Really enjoyed it",
                                "rating", 4.5,
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.data.title").value("Great Game"))
                .andExpect(jsonPath("$.data.content").value("Really enjoyed it"))
                .andExpect(jsonPath("$.data.rating").value(4.5))
                .andExpect(jsonPath("$.data.gameId").value(game.getId()));
    }

    @Test
    @DisplayName("리뷰 작성: 비로그인 → 401")
    void writeReview_unauthorized() throws Exception {
        Game game = createTestGame();

        mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Title",
                                "content", "Content",
                                "rating", 4.0,
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 작성: 라이브러리에 없는 게임 → 403-3")
    void writeReview_gameNotInLibrary() throws Exception {
        String email = uniqueEmail("writer");
        String nickname = uniqueNickname("writer");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");

        Game game = createTestGame();

        mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Title",
                                "content", "Content",
                                "rating", 4.0,
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-3"));
    }

    @Test
    @DisplayName("리뷰 작성: 중복 작성 → 400-1")
    void writeReview_duplicate() throws Exception {
        String email = uniqueEmail("writer");
        String nickname = uniqueNickname("writer");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        addGameToLibrary(cookies, memberId, game.getId());
        writeReview(cookies, game.getId(), "First Review", "Content", 4.0);

        mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Second Review",
                                "content", "Content again",
                                "rating", 3.0,
                                "gameId", game.getId()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("리뷰 작성: 없는 게임 → 404-1")
    void writeReview_gameNotFound() throws Exception {
        String email = uniqueEmail("writer");
        String nickname = uniqueNickname("writer");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");

        mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Title",
                                "content", "Content",
                                "rating", 4.0,
                                "gameId", 999999
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("리뷰 작성: 유효성 검사 실패 (빈 title) → 400-1")
    void writeReview_validationFail() throws Exception {
        String email = uniqueEmail("writer");
        String nickname = uniqueNickname("writer");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");

        mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "",
                                "content", "Content",
                                "rating", 4.0,
                                "gameId", 1
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("내 리뷰 조회(게임별): 성공")
    void getMyGameReview_success() throws Exception {
        String email = uniqueEmail("myreview");
        String nickname = uniqueNickname("myreview");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        addGameToLibrary(cookies, memberId, game.getId());
        writeReview(cookies, game.getId(), "My Review", "My content", 4.0);

        mvc.perform(get("/api/v1/reviews/my/game/{gameId}", game.getId())
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.title").value("My Review"))
                .andExpect(jsonPath("$.data.content").value("My content"));
    }

    @Test
    @DisplayName("내 리뷰 조회(게임별): 리뷰 없음 → 404-2")
    void getMyGameReview_notFound() throws Exception {
        String email = uniqueEmail("myreview");
        String nickname = uniqueNickname("myreview");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");

        Game game = createTestGame();

        mvc.perform(get("/api/v1/reviews/my/game/{gameId}", game.getId())
                        .cookie(cookies))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"));
    }

    @Test
    @DisplayName("내 리뷰 조회(게임별): 비로그인 → 401")
    void getMyGameReview_unauthorized() throws Exception {
        mvc.perform(get("/api/v1/reviews/my/game/{gameId}", 1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 수정: 성공")
    void modifyReview_success() throws Exception {
        String email = uniqueEmail("modifier");
        String nickname = uniqueNickname("modifier");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        addGameToLibrary(cookies, memberId, game.getId());

        writeReview(cookies, game.getId(), "Original Title", "Original Content", 3.0);
        Member author = memberRepository.findByEmail(email);
        int reviewId = reviewRepository.findByAuthorAndGame(author, game).get().getId();

        mvc.perform(put("/api/v1/reviews/{id}", reviewId)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Modified Title",
                                "content", "Modified Content",
                                "rating", 5.0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.data.title").value("Modified Title"))
                .andExpect(jsonPath("$.data.content").value("Modified Content"))
                .andExpect(jsonPath("$.data.rating").value(5.0));
    }

    @Test
    @DisplayName("리뷰 수정: 비로그인 → 401")
    void modifyReview_unauthorized() throws Exception {
        mvc.perform(put("/api/v1/reviews/{id}", 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Title",
                                "content", "Content",
                                "rating", 4.0
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 수정: 유효성 검사 실패 (빈 title) → 400-1")
    void modifyReview_validationFail() throws Exception {
        String email = uniqueEmail("modifier");
        String nickname = uniqueNickname("modifier");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");

        mvc.perform(put("/api/v1/reviews/{id}", 1)
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "",
                                "content", "",
                                "rating", 4.0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("리뷰 수정: 다른 유저의 리뷰 수정 → 403-1")
    void modifyReview_forbidden() throws Exception {
        String writerEmail = uniqueEmail("writer");
        String writerNickname = uniqueNickname("writer");
        signup(writerEmail, "1234", writerNickname);
        Cookie[] writerCookies = loginAndGetCookies(writerEmail, "1234");
        int writerId = memberRepository.findByEmail(writerEmail).getId();

        String otherEmail = uniqueEmail("other");
        String otherNickname = uniqueNickname("other");
        signup(otherEmail, "1234", otherNickname);
        Cookie[] otherCookies = loginAndGetCookies(otherEmail, "1234");

        Game game = createTestGame();
        addGameToLibrary(writerCookies, writerId, game.getId());

        MvcResult writeResult = writeReview(writerCookies, game.getId(), "Writer's Review", "Content", 4.0);
        int reviewId = om.readTree(writeResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        mvc.perform(put("/api/v1/reviews/{id}", reviewId)
                        .with(csrf())
                        .cookie(otherCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "title", "Hacked Title",
                                "content", "Hacked Content",
                                "rating", 1.0
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("리뷰 삭제: 성공")
    void deleteReview_success() throws Exception {
        String email = uniqueEmail("deleter");
        String nickname = uniqueNickname("deleter");
        signup(email, "1234", nickname);
        Cookie[] cookies = loginAndGetCookies(email, "1234");
        int memberId = memberRepository.findByEmail(email).getId();

        Game game = createTestGame();
        addGameToLibrary(cookies, memberId, game.getId());

        MvcResult writeResult = writeReview(cookies, game.getId(), "To Delete", "Content", 4.0);
        int reviewId = om.readTree(writeResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        mvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                        .with(csrf())
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));
    }

    @Test
    @DisplayName("리뷰 삭제: 비로그인 → 401")
    void deleteReview_unauthorized() throws Exception {
        mvc.perform(delete("/api/v1/reviews/{id}", 1)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 삭제: 다른 유저의 리뷰 삭제 → 403-2")
    void deleteReview_forbidden() throws Exception {
        String writerEmail = uniqueEmail("writer");
        String writerNickname = uniqueNickname("writer");
        signup(writerEmail, "1234", writerNickname);
        Cookie[] writerCookies = loginAndGetCookies(writerEmail, "1234");
        int writerId = memberRepository.findByEmail(writerEmail).getId();

        String otherEmail = uniqueEmail("other");
        String otherNickname = uniqueNickname("other");
        signup(otherEmail, "1234", otherNickname);
        Cookie[] otherCookies = loginAndGetCookies(otherEmail, "1234");

        Game game = createTestGame();
        addGameToLibrary(writerCookies, writerId, game.getId());

        MvcResult writeResult = writeReview(writerCookies, game.getId(), "Writer's Review", "Content", 4.0);
        int reviewId = om.readTree(writeResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        mvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                        .with(csrf())
                        .cookie(otherCookies))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-2"));
    }
}
