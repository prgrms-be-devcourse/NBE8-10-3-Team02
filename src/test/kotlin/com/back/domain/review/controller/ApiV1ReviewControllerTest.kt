package com.back.domain.review.controller

import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.game.service.GenreSyncService
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.review.repository.ReviewRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1ReviewControllerTest {
    @Autowired lateinit var mvc: MockMvc

    @Autowired lateinit var memberRepository: MemberRepository

    @Autowired lateinit var gameRepository: GameRepository

    @Autowired lateinit var reviewRepository: ReviewRepository

    @MockitoBean
    lateinit var genreSyncService: GenreSyncService

    private val om = ObjectMapper()

    // --- helpers ---

    private fun uniqueEmail(prefix: String) = "${prefix}_${System.nanoTime()}@test.com"

    private fun uniqueNickname(prefix: String) = "${prefix}_${System.nanoTime()}"

    private fun signup(
        email: String,
        password: String,
        nickname: String,
    ) {
        mvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "email" to email,
                                "password" to password,
                                "nickname" to nickname,
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
    }

    private fun loginAndGetCookies(
        email: String,
        password: String,
    ): Array<Cookie> {
        val result =
            mvc
                .perform(
                    post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(mapOf("email" to email, "password" to password))),
                ).andExpect(status().isOk)
                .andReturn()
        return result.response.cookies
    }

    private fun createTestGame(): Game {
        val game =
            Game.createGame(
                System.nanoTime(),
                "Test Game ${System.nanoTime()}",
                "Test Summary",
                null,
                LocalDate.now(),
            )
        return gameRepository.save(game)
    }

    private fun addGameToLibrary(
        cookies: Array<Cookie>,
        memberId: Int,
        gameId: Int,
    ) {
        mvc
            .perform(
                post("/api/v1/members/{memberId}/library", memberId)
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "platform" to "PC",
                                "playtime" to 0.0,
                                "isFavorite" to false,
                                "status" to "PLAYING",
                                "gameId" to gameId,
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
    }

    private fun writeReview(
        cookies: Array<Cookie>,
        gameId: Int,
        title: String,
        content: String,
        rating: Double,
    ): MvcResult =
        mvc
            .perform(
                post("/api/v1/reviews")
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf("title" to title, "content" to content, "rating" to rating, "gameId" to gameId),
                        ),
                    ),
            ).andReturn()

    // --- tests ---

    @Test
    @DisplayName("리뷰 목록 조회: 200-1 반환")
    fun getReviews_success() {
        mvc
            .perform(get("/api/v1/reviews"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("회원별 리뷰 목록 조회: 200-1 반환")
    fun getReviewsByMember_success() {
        val email = uniqueEmail("reviewer")
        val nickname = uniqueNickname("reviewer")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")
        val memberId = requireNotNull(memberRepository.findByEmail(email)).id

        mvc
            .perform(get("/api/v1/reviews/member/{memberId}", memberId).cookie(*cookies))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("게임별 리뷰 목록 조회: 200-1 반환")
    fun getReviewsByGame_success() {
        val game = createTestGame()

        mvc
            .perform(get("/api/v1/reviews/game/{gameId}", game.getId()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("리뷰 단건 조회: 성공")
    fun getReview_success() {
        val email = uniqueEmail("reviewer")
        val nickname = uniqueNickname("reviewer")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")
        val memberId = requireNotNull(memberRepository.findByEmail(email)).id

        val game = createTestGame()
        addGameToLibrary(cookies, memberId, game.getId())

        val writeResult = writeReview(cookies, game.getId(), "Test Title", "Test Content", 4.5)
        val reviewId =
            om
                .readTree(writeResult.response.contentAsString)
                .get("data")
                .get("id")
                .asInt()

        mvc
            .perform(get("/api/v1/reviews/{id}", reviewId).cookie(*cookies))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Test Title"))
            .andExpect(jsonPath("$.content").value("Test Content"))
            .andExpect(jsonPath("$.rating").value(4.5))
    }

    @Test
    @DisplayName("리뷰 단건 조회: 없는 ID → 404-1")
    fun getReview_notFound() {
        val email = uniqueEmail("reviewer")
        signup(email, "1234", uniqueNickname("reviewer"))
        val cookies = loginAndGetCookies(email, "1234")

        mvc
            .perform(get("/api/v1/reviews/{id}", 999999).cookie(*cookies))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("리뷰 작성: 성공 (라이브러리에 게임 있음)")
    fun writeReview_success() {
        val email = uniqueEmail("writer")
        val nickname = uniqueNickname("writer")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")
        val memberId = requireNotNull(memberRepository.findByEmail(email)).id

        val game = createTestGame()
        addGameToLibrary(cookies, memberId, game.getId())

        mvc
            .perform(
                post("/api/v1/reviews")
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "title" to "Great Game",
                                "content" to "Really enjoyed it",
                                "rating" to 4.5,
                                "gameId" to game.getId(),
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.resultCode").value("201"))
            .andExpect(jsonPath("$.data.title").value("Great Game"))
            .andExpect(jsonPath("$.data.content").value("Really enjoyed it"))
            .andExpect(jsonPath("$.data.rating").value(4.5))
            .andExpect(jsonPath("$.data.gameId").value(game.getId()))
    }

    @Test
    @DisplayName("리뷰 작성: 비로그인 → 401")
    fun writeReview_unauthorized() {
        val game = createTestGame()

        mvc
            .perform(
                post("/api/v1/reviews")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "title" to "Title",
                                "content" to "Content",
                                "rating" to 4.0,
                                "gameId" to game.getId(),
                            ),
                        ),
                    ),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("리뷰 작성: 라이브러리에 없는 게임 → 403-3")
    fun writeReview_gameNotInLibrary() {
        val email = uniqueEmail("writer")
        val nickname = uniqueNickname("writer")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")

        val game = createTestGame()

        mvc
            .perform(
                post("/api/v1/reviews")
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "title" to "Title",
                                "content" to "Content",
                                "rating" to 4.0,
                                "gameId" to game.getId(),
                            ),
                        ),
                    ),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-3"))
    }

    @Test
    @DisplayName("리뷰 작성: 중복 작성 → 400-1")
    fun writeReview_duplicate() {
        val email = uniqueEmail("writer")
        val nickname = uniqueNickname("writer")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")
        val memberId = requireNotNull(memberRepository.findByEmail(email)).id

        val game = createTestGame()
        addGameToLibrary(cookies, memberId, game.getId())
        writeReview(cookies, game.getId(), "First Review", "Content", 4.0)

        mvc
            .perform(
                post("/api/v1/reviews")
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "title" to "Second Review",
                                "content" to "Content again",
                                "rating" to 3.0,
                                "gameId" to game.getId(),
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("리뷰 작성: 없는 게임 → 404-1")
    fun writeReview_gameNotFound() {
        val email = uniqueEmail("writer")
        val nickname = uniqueNickname("writer")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")

        mvc
            .perform(
                post("/api/v1/reviews")
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf("title" to "Title", "content" to "Content", "rating" to 4.0, "gameId" to 999999),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("리뷰 작성: 유효성 검사 실패 (빈 title) → 400-1")
    fun writeReview_validationFail() {
        val email = uniqueEmail("writer")
        val nickname = uniqueNickname("writer")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")

        mvc
            .perform(
                post("/api/v1/reviews")
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf("title" to "", "content" to "Content", "rating" to 4.0, "gameId" to 1),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("내 리뷰 조회(게임별): 성공")
    fun getMyGameReview_success() {
        val email = uniqueEmail("myreview")
        val nickname = uniqueNickname("myreview")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")
        val memberId = requireNotNull(memberRepository.findByEmail(email)).id

        val game = createTestGame()
        addGameToLibrary(cookies, memberId, game.getId())
        writeReview(cookies, game.getId(), "My Review", "My content", 4.0)

        mvc
            .perform(get("/api/v1/reviews/my/game/{gameId}", game.getId()).cookie(*cookies))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.title").value("My Review"))
            .andExpect(jsonPath("$.data.content").value("My content"))
    }

    @Test
    @DisplayName("내 리뷰 조회(게임별): 리뷰 없음 → 404-2")
    fun getMyGameReview_notFound() {
        val email = uniqueEmail("myreview")
        val nickname = uniqueNickname("myreview")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")

        val game = createTestGame()

        mvc
            .perform(get("/api/v1/reviews/my/game/{gameId}", game.getId()).cookie(*cookies))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-2"))
    }

    @Test
    @DisplayName("내 리뷰 조회(게임별): 비로그인 → 401")
    fun getMyGameReview_unauthorized() {
        mvc
            .perform(get("/api/v1/reviews/my/game/{gameId}", 1))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("리뷰 수정: 성공")
    fun modifyReview_success() {
        val email = uniqueEmail("modifier")
        val nickname = uniqueNickname("modifier")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")
        val memberId = requireNotNull(memberRepository.findByEmail(email)).id

        val game = createTestGame()
        addGameToLibrary(cookies, memberId, game.getId())

        writeReview(cookies, game.getId(), "Original Title", "Original Content", 3.0)
        val author = requireNotNull(memberRepository.findByEmail(email))
        val reviewId = reviewRepository.findByAuthorAndGame(author, game)!!.id

        mvc
            .perform(
                put("/api/v1/reviews/{id}", reviewId)
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "title" to "Modified Title",
                                "content" to "Modified Content",
                                "rating" to 5.0,
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.resultCode").value("201"))
            .andExpect(jsonPath("$.data.title").value("Modified Title"))
            .andExpect(jsonPath("$.data.content").value("Modified Content"))
            .andExpect(jsonPath("$.data.rating").value(5.0))
    }

    @Test
    @DisplayName("리뷰 수정: 비로그인 → 401")
    fun modifyReview_unauthorized() {
        mvc
            .perform(
                put("/api/v1/reviews/{id}", 1)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(mapOf("title" to "Title", "content" to "Content", "rating" to 4.0))),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("리뷰 수정: 유효성 검사 실패 (빈 title) → 400-1")
    fun modifyReview_validationFail() {
        val email = uniqueEmail("modifier")
        val nickname = uniqueNickname("modifier")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")

        mvc
            .perform(
                put("/api/v1/reviews/{id}", 1)
                    .with(csrf())
                    .cookie(*cookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(mapOf("title" to "", "content" to "", "rating" to 4.0))),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("리뷰 수정: 다른 유저의 리뷰 수정 → 403-1")
    fun modifyReview_forbidden() {
        val writerEmail = uniqueEmail("writer")
        val writerNickname = uniqueNickname("writer")
        signup(writerEmail, "1234", writerNickname)
        val writerCookies = loginAndGetCookies(writerEmail, "1234")
        val writerId = requireNotNull(memberRepository.findByEmail(writerEmail)).id

        val otherEmail = uniqueEmail("other")
        val otherNickname = uniqueNickname("other")
        signup(otherEmail, "1234", otherNickname)
        val otherCookies = loginAndGetCookies(otherEmail, "1234")

        val game = createTestGame()
        addGameToLibrary(writerCookies, writerId, game.getId())

        val writeResult = writeReview(writerCookies, game.getId(), "Writer's Review", "Content", 4.0)
        val reviewId =
            om
                .readTree(writeResult.response.contentAsString)
                .get("data")
                .get("id")
                .asInt()

        mvc
            .perform(
                put("/api/v1/reviews/{id}", reviewId)
                    .with(csrf())
                    .cookie(*otherCookies)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        om.writeValueAsString(
                            mapOf(
                                "title" to "Hacked Title",
                                "content" to "Hacked Content",
                                "rating" to 1.0,
                            ),
                        ),
                    ),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @DisplayName("리뷰 삭제: 성공")
    fun deleteReview_success() {
        val email = uniqueEmail("deleter")
        val nickname = uniqueNickname("deleter")
        signup(email, "1234", nickname)
        val cookies = loginAndGetCookies(email, "1234")
        val memberId = requireNotNull(memberRepository.findByEmail(email)).id

        val game = createTestGame()
        addGameToLibrary(cookies, memberId, game.getId())

        val writeResult = writeReview(cookies, game.getId(), "To Delete", "Content", 4.0)
        val reviewId =
            om
                .readTree(writeResult.response.contentAsString)
                .get("data")
                .get("id")
                .asInt()

        mvc
            .perform(
                delete("/api/v1/reviews/{id}", reviewId)
                    .with(csrf())
                    .cookie(*cookies),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
    }

    @Test
    @DisplayName("리뷰 삭제: 비로그인 → 401")
    fun deleteReview_unauthorized() {
        mvc
            .perform(
                delete("/api/v1/reviews/{id}", 1)
                    .with(csrf()),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("리뷰 삭제: 다른 유저의 리뷰 삭제 → 403-2")
    fun deleteReview_forbidden() {
        val writerEmail = uniqueEmail("writer")
        val writerNickname = uniqueNickname("writer")
        signup(writerEmail, "1234", writerNickname)
        val writerCookies = loginAndGetCookies(writerEmail, "1234")
        val writerId = requireNotNull(memberRepository.findByEmail(writerEmail)).id

        val otherEmail = uniqueEmail("other")
        val otherNickname = uniqueNickname("other")
        signup(otherEmail, "1234", otherNickname)
        val otherCookies = loginAndGetCookies(otherEmail, "1234")

        val game = createTestGame()
        addGameToLibrary(writerCookies, writerId, game.getId())

        val writeResult = writeReview(writerCookies, game.getId(), "Writer's Review", "Content", 4.0)
        val reviewId =
            om
                .readTree(writeResult.response.contentAsString)
                .get("data")
                .get("id")
                .asInt()

        mvc
            .perform(
                delete("/api/v1/reviews/{id}", reviewId)
                    .with(csrf())
                    .cookie(*otherCookies),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-2"))
    }
}
