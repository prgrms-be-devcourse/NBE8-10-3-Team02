package com.back.domain.review.controller

import com.back.domain.game.game.service.GameService
import com.back.domain.member.member.service.MemberService
import com.back.domain.member.memberGame.service.MemberGameService
import com.back.domain.review.dto.ReviewDto
import com.back.domain.review.dto.ReviewModifyRequest
import com.back.domain.review.dto.ReviewWriteRequest
import com.back.domain.review.service.ReviewService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "ApiV1ReviewController", description = "API 리뷰 컨트롤러")
class ApiV1ReviewController(
    private val reviewService: ReviewService,
    private val memberService: MemberService,
    private val gameService: GameService,
    private val memberGameService: MemberGameService,
    private val rq: Rq,
) {
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    fun getReviews(
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): RsData<Page<ReviewDto>> {
        val reviews = reviewService.findAll(pageable).map(::ReviewDto)
        return RsData("200-1", "리뷰 목록 조회", reviews)
    }

    @GetMapping("/member/{memberId}")
    @Transactional(readOnly = true)
    @Operation(summary = "한 유저의 모든 리뷰 조회")
    fun getReviewsByMember(
        @PathVariable memberId: Long,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): RsData<Page<ReviewDto>> {
        val reviews = reviewService.findByAuthorId(memberId, pageable).map(::ReviewDto)
        return RsData("200-1", "유저별 리뷰 목록 조회", reviews)
    }

    @GetMapping("/game/{gameId}")
    @Transactional(readOnly = true)
    @Operation(summary = "한 게임의 모든 리뷰 조회")
    fun getReviewsByGame(
        @PathVariable gameId: Long,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): RsData<Page<ReviewDto>> {
        val reviews = reviewService.findByGameId(gameId, pageable).map(::ReviewDto)
        return RsData("200-1", "게임별 리뷰 목록 조회", reviews)
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    fun getReview(
        @PathVariable id: Int,
    ): ReviewDto {
        val review = reviewService.findById(id) ?: throw NoSuchElementException()
        return ReviewDto(review)
    }

    @PostMapping
    @Transactional
    @Operation(summary = "작성")
    fun write(
        @Valid @RequestBody reqBody: ReviewWriteRequest,
    ): RsData<ReviewDto> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        val game = gameService.findById(reqBody.gameId).orElseThrow { ServiceException("404-1", "No Game") }
        val review = reviewService.write(reqBody.title, reqBody.content, reqBody.rating, actor, game)
        memberGameService.updateReview(actor.id, game.getId(), review)
        return RsData("201", "리뷰가 작성되었습니다.", ReviewDto(review))
    }

    @GetMapping("/my/game/{gameId}")
    @Transactional(readOnly = true)
    @Operation(summary = "내 리뷰 조회 (게임별)")
    fun getMyGameReview(
        @PathVariable gameId: Int,
    ): RsData<ReviewDto> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        val game =
            gameService.findById(gameId).orElseThrow {
                ServiceException("404-1", "해당 게임이 존재하지 않습니다.")
            }
        val review =
            reviewService.findByAuthorAndGame(actor, game)
                ?: throw ServiceException("404-2", "작성한 리뷰가 없습니다.")
        return RsData("200-1", "내 리뷰 조회", ReviewDto(review))
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "수정")
    fun modify(
        @PathVariable id: Int,
        @Valid @RequestBody reqBody: ReviewModifyRequest,
    ): RsData<ReviewDto> {
        val actorPrincipal = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        val actor = memberService.findById(actorPrincipal.id) ?: throw NoSuchElementException()
        val review = reviewService.findById(id) ?: throw NoSuchElementException()
        review.checkActorCanModify(actor)
        reviewService.modify(review, reqBody.title, reqBody.content, reqBody.rating)
        return RsData("201", "리뷰가 수정되었습니다.", ReviewDto(review))
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "삭제")
    fun delete(
        @PathVariable id: Int,
    ): RsData<Void> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        val review = reviewService.findById(id) ?: throw NoSuchElementException()
        review.checkActorCanDelete(actor)
        reviewService.delete(review)
        return RsData("200", "${id}번 리뷰가 삭제되었습니다.")
    }
}
