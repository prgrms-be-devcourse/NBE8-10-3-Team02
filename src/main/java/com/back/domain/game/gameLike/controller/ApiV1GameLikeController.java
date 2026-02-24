package com.back.domain.game.gameLike.controller;

import com.back.domain.game.gameLike.dto.GameLikeResponse;
import com.back.domain.game.gameLike.service.GameLikeService;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
// import lombok.RequiredArgsConstructor; // 1. 삭제
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/games")
// @RequiredArgsConstructor // 2. 삭제
@Tag(name = "ApiV1GameLikeController", description = "게임 좋아요 API")
public class ApiV1GameLikeController {
    private final GameLikeService gameLikeService;
    private final Rq rq;

    // 3. 수동 생성자 추가 (코틀린이 이 생성자를 보고 빌드를 허용해줍니다)
    public ApiV1GameLikeController(GameLikeService gameLikeService, Rq rq) {
        this.gameLikeService = gameLikeService;
        this.rq = rq;
    }

    @PostMapping("/{igdbId}/like")
    @Operation(summary = "좋아요 토글", description = "좋아요가 없으면 추가, 있으면 취소")
    public RsData<GameLikeResponse> toggleLike(@PathVariable long igdbId) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        boolean liked = gameLikeService.toggleLike(actor, igdbId);
        long likeCount = gameLikeService.getLikeCount(igdbId);

        String msg = liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";

        return new RsData<>(
                "200-1",
                msg,
                GameLikeResponse.from(igdbId, liked, likeCount)
        );
    }

    @GetMapping("/{igdbId}/like")
    @Operation(summary = "좋아요 상태 조회", description = "현재 사용자의 좋아요 여부와 총 좋아요 수")
    public GameLikeResponse getLikeStatus(@PathVariable long igdbId) {
        Member actor = rq.getActor();
        boolean liked = gameLikeService.isLiked(actor, igdbId);
        long likeCount = gameLikeService.getLikeCount(igdbId);

        return GameLikeResponse.from(igdbId, liked, likeCount);
    }
}