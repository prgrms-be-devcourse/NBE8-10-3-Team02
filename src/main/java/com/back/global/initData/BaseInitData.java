package com.back.global.initData;

import com.back.domain.game.game.service.GameService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.post.post.service.PostService; // PostService 임포트 필요
import com.back.global.app.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    private final MemberService memberService;
    private final GameService gameService;
    private final PostService postService; // 게시글 서비스 추가

    @Bean
    @Order(1) // 멤버 먼저 생성
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            work1(); // 멤버 생성
            work2(); // 게시글 생성
        };
    }

    @Transactional
    public void work1() {
        if (AppConfig.isProd()) return;

        tryJoin("admin@test.com", "1234", "관리자");
        tryJoin("user1@test.com", "1234", "유저1");
        tryJoin("user2@test.com", "1234", "유저2");
        // 배치 동기화와 충돌 방지를 위해 주석 처리
        // try {
        //     gameService.createGame(123L,
        //             "Cat Mario",
        //             "고양이 마리오 게임",
        //             "cat_mario.png",
        //             LocalDate.now());
        //     gameService.createGame(124L,
        //             "Super Mario Bros.",
        //             "최초의 마리오 게임",
        //             "super_mario_bros.png",
        //             LocalDate.of(1985, 9,13));
        // } catch (Exception ignored) {
        // }

    }

    @Transactional
    public void work2() {
        if (AppConfig.isProd()) return;

        Member user1 = memberService.findByEmail("user1@test.com");
        Member user2 = memberService.findByEmail("user2@test.com");

        if (user1 == null || user2 == null) return;

        postService.write(user1, "롤 한판 하실 분 구함", "실력 상관없음", List.of("롤", "리그오브레전드", "랭크"));
        postService.write(user1, "배그 스쿼드 모집", "마이크 필수", List.of("배그", "FPS"));

        postService.write(user2, "조회123", "테스트 내용입니다.", List.of("조회태그222"));
        postService.write(user2, "조회", "두 번째 테스트입니다.", List.of("조회"));
        postService.write(user1, "조회 필터링 테스트", "세 번째 테스트입니다.", List.of("조회태그"));

        postService.write(user2, "자유게시판 제목", "내용 없음", List.of("RPG", "로스트아크"));
        postService.write(user1, "정보 공유", "꿀팁 방출합니다.", List.of("RPG", "메이플"));
    }

    private void tryJoin(String email, String password, String nickname) {
        try {
            memberService.join(email, password, nickname);
        } catch (Exception ignored) {
        }
    }
}