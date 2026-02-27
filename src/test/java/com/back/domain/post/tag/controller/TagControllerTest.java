package com.back.domain.post.tag.controller;

import com.back.domain.tag.tag.entity.Tag;
import com.back.domain.tag.tag.service.TagService;
import com.back.global.rq.Rq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TagControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private Rq rq;

    @BeforeEach
    void setUp() {
        // 필터 NPE 방지 설정
        lenient().when(rq.getHeader(anyString(), anyString())).thenReturn("");
        lenient().when(rq.getCookieValue(anyString(), anyString())).thenReturn("");
    }

    @Test
    @DisplayName("태그 전체 조회 성공")
    void get_tags_success() throws Exception {
        Tag tag1 = new Tag("Action");
        Tag tag2 = new Tag("RPG");
        given(tagService.findAll()).willReturn(List.of(tag1, tag2));

        mvc.perform(get("/api/v1/tags"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Action"));
    }

    @Test
    @DisplayName("태그 생성 성공 (201)")
    void create_tag_success() throws Exception {
        Tag tag = new Tag("Steam");
        given(tagService.create(anyString())).willReturn(tag);

        Map<String, String> body = Map.of("content", "Steam");

        mvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated()) // RsData 201-1 대응
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.content").value("Steam"));
    }

    @Test
    @DisplayName("태그 삭제 성공")
    void delete_tag_success() throws Exception {
        Tag tag = new Tag("DeleteMe");
        ReflectionTestUtils.setField(tag, "id", 1);
        
        given(tagService.findById(1)).willReturn(Optional.of(tag));

        mvc.perform(delete("/api/v1/tags/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("1번 태그가 삭제되었습니다."));
    }

    @Test
    @DisplayName("IGDB 아이디로 태그 생성 성공")
    void create_from_igdb_success() throws Exception {
        Tag tag1 = new Tag("Adventure");
        Tag tag2 = new Tag("Indie");
        given(tagService.createTagsFromIgdb(anyLong())).willReturn(List.of(tag1, tag2));

        mvc.perform(post("/api/v1/tags/igdb/12345"))
                .andDo(print())
                .andExpect(status().isCreated()) // RsData 201-2 대응
                .andExpect(jsonPath("$.resultCode").value("201-2"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("존재하지 않는 태그 삭제 시 404")
    void delete_tag_fail_404() throws Exception {
        given(tagService.findById(anyInt())).willReturn(Optional.empty());

        mvc.perform(delete("/api/v1/tags/999"))
                .andDo(print())
                .andExpect(status().isNotFound()) // ServiceException 404-1 대응
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
