package com.back.domain.post.post.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.post.dto.PostDto;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.global.rq.Rq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private Rq rq;

    @BeforeEach
    void setUp() {

        lenient().when(rq.getHeader(anyString(), anyString())).thenReturn("");
        lenient().when(rq.getCookieValue(anyString(), anyString())).thenReturn("");
    }

    private Member getMockMember() {
        return new Member("user1@test.com", "1234", "유저1");
    }


    @Test
    @DisplayName("게시글 목록 조회 성공: 200 OK")
    void get_items_success() throws Exception {
        Member actor = getMockMember();
        Post post = new Post(actor, "제목", "내용");
        // Page 객체 생성을 위해 PageImpl 사용
        PageImpl<Post> page = new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1);

        given(postService.search(anyString(), anyString(), any())).willReturn(page);

        mvc.perform(get("/api/v1/posts")
                        .param("kw", "제목")
                        .param("page", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                // RsData -> Page -> content 순서이므로 경로 주의
                .andExpect(jsonPath("$.data.content[0].title").value("제목"));
    }


    @Test
    @DisplayName("게시글 단건 조회 성공: 200 OK")
    void get_item_success() throws Exception {
        Member actor = getMockMember();
        Post post = new Post(actor, "조회용", "내용");
        given(postService.findById(1)).willReturn(post);

        mvc.perform(get("/api/v1/posts/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("조회용"));
    }


    @Test
    @DisplayName("게시글 작성 성공: 201 Created")
    @WithMockUser
    void create_success() throws Exception {
        Member actor = getMockMember();
        given(rq.getActor()).willReturn(actor);
        given(memberService.findById(anyInt())).willReturn(actor);
        given(postService.write(any(), anyString(), anyString(), anyList()))
                .willReturn(new Post(actor, "새 제목", "새 내용"));

        Map<String, Object> body = Map.of("title", "새 제목", "content", "새 내용", "tags", List.of());

        mvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"));
    }


    @Test
    @DisplayName("게시글 수정 성공: 200 OK")
    @WithMockUser(username = "1", roles = "USER")
    void modify_success() throws Exception {
        // Given
        Member actor = getMockMember();
        Post post = new Post(actor, "기존 제목", "기존 내용");

        given(rq.getActor()).willReturn(actor);
        given(postService.findById(1)).willReturn(post);
        given(memberService.findById(anyInt())).willReturn(actor);


        Map<String, Object> body = Map.of(
                "title", "수정된 제목",
                "content", "수정된 내용",
                "tags", List.of("tag1", "tag3")
        );


        mvc.perform(put("/api/v1/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print()) // 400 에러의 구체적인 이유(필드 에러)가 로그에 찍힙니다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("게시글 삭제 성공: 200 OK")
    @WithMockUser
    void delete_success() throws Exception {
        Member actor = getMockMember();
        Post post = new Post(actor, "삭제용", "내용");

        given(rq.getActor()).willReturn(actor);
        given(postService.findById(1)).willReturn(post);
        given(memberService.findById(anyInt())).willReturn(actor);

        mvc.perform(delete("/api/v1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }


    @Test
    @DisplayName("좋아요 토글 성공: 200 OK")
    @WithMockUser
    void like_success() throws Exception {
        Member actor = getMockMember();
        given(rq.getActor()).willReturn(actor);
        given(postService.toggleLike(any(), anyInt())).willReturn(true);
        given(postService.getLikeCount(1)).willReturn(10L);

        mvc.perform(post("/api/v1/posts/1/like"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(10))
                .andExpect(jsonPath("$.msg").value("좋아요를 눌렀습니다."));
    }
}
