package com.back.domain.game.recommendation.controller

import com.back.domain.game.recommendation.dto.GameRecommendationResponse
import com.back.domain.game.recommendation.service.GameRecommendationService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Tag(name = "ApiV1RecommendationController", description = "게임 추천 API")
class ApiV1RecommendationController(
    private val gameRecommendationService: GameRecommendationService,
    private val rq: Rq,
) {
    @GetMapping("/recommendations")
    @Operation(summary = "개인 맞춤 게임 추천")
    fun getRecommendations(
        @RequestParam(defaultValue = "20") limit: Int,
    ): RsData<List<GameRecommendationResponse>> {
        val actor =
            rq.getActor()
                ?: throw ServiceException("401-1", "로그인이 필요합니다.")

        val recommendations = gameRecommendationService.getPersonalRecommendations(actor.id, limit)
        return RsData("200-1", "추천 게임 조회 성공", recommendations)
    }

    @GetMapping("/games/{igdbId}/similar")
    @Operation(summary = "유사 게임 추천")
    fun getSimilarGames(
        @PathVariable igdbId: Long,
        @RequestParam(defaultValue = "10") limit: Int,
    ): RsData<List<GameRecommendationResponse>> {
        val similarGames = gameRecommendationService.getSimilarGames(igdbId, limit)
        return RsData("200-1", "유사 게임 조회 성공", similarGames)
    }
}
