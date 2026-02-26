package com.back.domain.game.gameLike.controller

import com.back.domain.game.gameLike.dto.GameLikeResponse
import com.back.domain.game.gameLike.service.GameLikeService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "ApiV1GameLikeController", description = "게임 좋아요 API")
class ApiV1GameLikeController(
    private val gameLikeService: GameLikeService,
    private val rq: Rq,
) {
    @PostMapping("/{igdbId}/like")
    @Operation(summary = "좋아요 토글", description = "좋아요가 없으면 추가, 있으면 취소")
    fun toggleLike(
        @PathVariable igdbId: Long,
    ): RsData<GameLikeResponse> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")

        val liked = gameLikeService.toggleLike(actor, igdbId)
        val likeCount = gameLikeService.getLikeCount(igdbId)
        val msg = if (liked) "좋아요를 눌렀습니다." else "좋아요를 취소했습니다."

        return RsData("200-1", msg, GameLikeResponse.from(igdbId, liked, likeCount))
    }

    @GetMapping("/{igdbId}/like")
    @Operation(summary = "좋아요 상태 조회", description = "현재 사용자의 좋아요 여부와 총 좋아요 수")
    fun getLikeStatus(
        @PathVariable igdbId: Long,
    ): GameLikeResponse {
        val actor = rq.actor
        val liked = gameLikeService.isLiked(actor, igdbId)
        val likeCount = gameLikeService.getLikeCount(igdbId)
        return GameLikeResponse.from(igdbId, liked, likeCount)
    }
}
