package com.back.domain.game.game.controller;

import com.back.domain.game.game.dto.GameSearchCondition;
import com.back.domain.game.game.dto.GameSearchResponse;
import com.back.domain.game.game.dto.GenreResponse;
import com.back.domain.game.game.service.GameSearchService;
import com.back.domain.game.game.service.GameService;
import com.back.domain.game.game.service.GenreService;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiV1GameController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ApiV1GameControllerSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private GameSearchService gameSearchService;

    @MockitoBean
    private GenreService genreService;

    // 추가: Security 필터(CustomAuthenticationFilter)가 의존하는 빈을 Mock 처리
    @MockitoBean
    private com.back.domain.member.member.service.MemberService memberService;

    // 2. 현재 에러 해결을 위한 Rq Mock 추가
    @MockitoBean
    private com.back.global.rq.Rq rq;



    @Test
    @DisplayName("게임 검색 API 테스트 - 성공")
    void searchGamesTest() throws Exception {
        // given
        // 에러 메시지에 명시된 순서: long, String, String, LocalDate, List<String>, List<String>
        GameSearchResponse response = new GameSearchResponse(
                1L,                                 // id
                "Zelda",                            // title
                "https://image.com/logo.png",       // thumbnail (예상)
                java.time.LocalDate.of(2023, 5, 12), // releaseDate
                List.of("Action", "Adventure"),     // genres
                List.of("Switch", "PC")             // platforms
        );

        given(gameSearchService.search(any(GameSearchCondition.class)))
                .willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/games/search")
                        .param("query", "Zelda")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 로그의 Body 결과에 맞춰 필드명 수정
                .andExpect(jsonPath("$[0].igdbId").value(1))   // gameId -> igdbId
                .andExpect(jsonPath("$[0].name").value("Zelda")) // title -> name
                .andExpect(jsonPath("$[0].imageUrl").exists())
                .andExpect(jsonPath("$[0].genres").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/games/search")
                        .param("query", "Zelda"))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("장르 목록 조회 테스트")
    void getGenresTest() throws Exception {
        // given
        GenreResponse genre = new GenreResponse(1L, "RPG");
        given(genreService.getGenres()).willReturn(List.of(genre));

        // when & then
        mockMvc.perform(get("/api/v1/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("RPG"));
    }

    @Test
    @DisplayName("플랫폼 목록 조회 테스트 - Enum 기반 데이터 검증")
    void getPlatformsTest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/platforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }
}