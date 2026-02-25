package com.back.domain.member.memberGame.controller

import com.back.domain.game.game.service.GameService
import com.back.domain.member.member.service.MemberService
import com.back.domain.member.memberGame.StatusEnum
import com.back.domain.member.memberGame.dto.MemberGameAddRequest
import com.back.domain.member.memberGame.dto.MemberGameDto
import com.back.domain.member.memberGame.dto.MemberGameUpdateRequest
import com.back.domain.member.memberGame.service.MemberGameService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members/{memberId}/library")
class ApiV1MemberGameController(
    private val gameService: GameService,
    private val memberService: MemberService,
    private val memberGameService: MemberGameService,
    private val rq: Rq,
) {
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회(회원별)")
    fun viewLibrary(
        @PathVariable memberId: Int,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) platform: String?,
    ): RsData<Page<MemberGameDto>> {
        if (memberId != rq.actor.id) throw ServiceException("403", "Cannot view this library")

        val statusEnum = status?.let { runCatching { StatusEnum.valueOf(it) }.getOrNull() }

        return RsData(
            "200-1",
            "라이브러리 조회",
            memberGameService.findByMemberIdWithFilters(memberId, statusEnum, platform, pageable)
                .map(::MemberGameDto),
        )
    }

    @PostMapping
    @Transactional
    @Operation(summary = "라이브러리에 멤버게임 추가")
    fun addToLibrary(
        @PathVariable memberId: Int,
        @RequestBody request: MemberGameAddRequest,
    ): RsData<MemberGameDto> {
        if (memberId != rq.actor.id) throw ServiceException("403", "Cannot add to this library")

        val actor = memberService.findById(rq.actor.id) ?: throw NoSuchElementException()
        val game = gameService.findById(request.gameId).orElseThrow { ServiceException("404-1", "No Game") }
        val memberGame = memberGameService.addToLibrary(
            request.platform, request.playtime, request.isFavorite, request.status, actor, game,
        )
        memberService.flush()
        return RsData("201-1", "라이브러리에 게임 ${game.name}가 추가되었습니다.", MemberGameDto(memberGame))
    }

    @PatchMapping("/{memberGameId}")
    @Transactional
    @Operation(summary = "라이브러리 게임 상태 업데이트")
    fun updateMemberGame(
        @PathVariable memberId: Int,
        @PathVariable memberGameId: Int,
        @Valid @RequestBody request: MemberGameUpdateRequest,
    ): RsData<MemberGameDto> {
        if (memberId != rq.actor.id) throw ServiceException("403", "Cannot update this library")

        val memberGame = memberGameService.updateMemberGame(memberGameId, memberId, request)
        return RsData("200", "게임 정보가 업데이트되었습니다.", MemberGameDto(memberGame))
    }

    @DeleteMapping("/{memberGameId}")
    @Transactional
    @Operation(summary = "라이브러리에서 게임삭제")
    fun removeFromLibrary(
        @PathVariable memberId: Int,
        @PathVariable memberGameId: Int,
    ): RsData<Void> {
        val actor = rq.actor
        if (memberId != actor.id) throw ServiceException("403", "Cannot delete from this library")

        val member = memberService.findById(actor.id) ?: throw NoSuchElementException()
        memberGameService.removeFromLibrary(member, memberGameId)
        memberService.flush()
        return RsData("204", "게임을 삭제하였습니다.")
    }
}
