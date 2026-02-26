package com.back.domain.post.postComment.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.entity.PostComment;
import com.back.domain.post.postComment.repository.PostCommentRepository;
import com.back.global.rq.Rq;
import com.back.global.security.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostCommentControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostCommentRepository postCommentRepository;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private Rq rq;

    private Member actor;
    private Post post;
    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        // 필터 NPE 방지
        lenient().when(rq.getHeader(anyString(), anyString())).thenReturn("");
        lenient().when(rq.getCookieValue(anyString(), anyString())).thenReturn("");

        // 기본 테스트 데이터 세팅
        actor = new Member("user1@test.com", "1234", "유저1");
        // Member 엔티티에 id 필드가 있을 경우 리플렉션이나 빌더로 id를 1로 세팅했다고 가정
        post = new Post(actor, "테스트 게시글", "내용");
        
        securityUser = new SecurityUser(1, actor.getEmail(), actor.getNickname(), "", List.of());
    }

    @Test
    @DisplayName("댓글 다건 조회 성공")
    void get_items_success() throws Exception {
        given(postService.findById(anyInt())).willReturn(post);

        mvc.perform(get("/api/v1/posts/1/comments"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void write_comment_success() throws Exception {
        given(postService.findById(anyInt())).willReturn(post);
        given(memberService.findById(anyInt())).willReturn(actor);

        // 가짜 댓글 객체 생성 (ID가 필요하다면 리플렉션 등으로 심어줘야 함)
        PostComment mockComment = new PostComment(actor, post, "댓글 내용", null);

        // 💡 4개의 인자를 모두 any() 처리합니다.
        // (author, post, content, parentId)
        given(postService.writeComment(any(), any(), anyString(), any()))
                .willReturn(mockComment);

        // parentId가 포함된 요청 바디 (DTO 구조에 맞춰서)
        Map<String, Object> body = Map.of(
                "content", "댓글 내용",
                "parentId", 0 // 또는 null
        );

        mvc.perform(post("/api/v1/posts/1/comments")
                        .with(user(securityUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                // 💡 여기를 .isOk() 대신 .isCreated()로 수정!
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"));
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void modify_comment_success() throws Exception {
        // 1. 작성자 및 로그인 유저 일치
        Member author = new Member("user1@test.com", "1234", "유저1");
        ReflectionTestUtils.setField(author, "id", 1);
        SecurityUser user = new SecurityUser(1, author.getEmail(), author.getNickname(), "", List.of());

        // 2. 게시글 생성
        Post post = new Post(author, "테스트 게시글", "내용");

        // 3. 댓글 생성 및 게시글에 수동으로 연결
        PostComment comment = new PostComment(author, post, "기존 댓글", null);
        ReflectionTestUtils.setField(comment, "id", 100);
        post.getComments().add(comment);

        // 4. Mock 설정
        given(postService.findById(anyInt())).willReturn(post);
        given(memberService.findById(anyInt())).willReturn(author);

        Map<String, Object> body = Map.of("content", "수정된 댓글 내용");

        // 5. 실행 (경로 변수 postId=1, commentId=100 확인)
        mvc.perform(put("/api/v1/posts/1/comments/100")
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void delete_comment_success() throws Exception {
        // 1. 작성자 생성 및 ID 부여
        Member author = new Member("user1@test.com", "1234", "유저1");
        ReflectionTestUtils.setField(author, "id", 1);

        // 2. 로그인 유저 정보 생성
        SecurityUser user = new SecurityUser(1, author.getEmail(), author.getNickname(), "", List.of());

        // 3. 댓글 생성
        PostComment comment = new PostComment(author, post, "삭제할 댓글", null);
        ReflectionTestUtils.setField(comment, "id", 100); // 댓글 자체의 ID

        // 4. Mock 설정
        given(postService.findById(anyInt())).willReturn(post);
        given(postCommentRepository.findById(anyInt())).willReturn(Optional.of(comment));

        // 5. 실행
        mvc.perform(delete("/api/v1/posts/1/comments/100")
                        .with(user(user))) // 로그인 유저 주입
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }
}
