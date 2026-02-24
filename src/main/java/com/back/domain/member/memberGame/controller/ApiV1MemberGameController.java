package com.back.domain.member.memberGame.controller;

import com.back.domain.game.game.entity.Game;
import com.back.domain.game.game.service.GameService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.memberGame.StatusEnum;
import com.back.domain.member.memberGame.dto.MemberGameUpdateRequest;
import com.back.domain.member.memberGame.dto.MemberGameAddRequest;
import com.back.domain.member.memberGame.dto.MemberGameDto;
import com.back.domain.member.memberGame.entity.MemberGame;

import com.back.domain.member.memberGame.service.MemberGameService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/{memberId}/library")
@RequiredArgsConstructor
public class ApiV1MemberGameController {
    private final GameService gameService;
    private final MemberService memberService;
    private final MemberGameService memberGameService;
    private final Rq rq;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회(회원별)")
    public RsData<Page<MemberGameDto>> viewLibrary(@PathVariable("memberId") int memberId,
                                               @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
                                           Pageable pageable,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String platform
    ) {
        if (memberId != rq.getActor().getId()){
            throw new ServiceException("403","Cannot view this library");
        }

        // Parse status string to enum if provided
        StatusEnum statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = StatusEnum.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status value, ignore filter
            }
        }

        Page<MemberGame> memberGames = memberGameService.findByMemberIdWithFilters(memberId, statusEnum, platform, pageable);
        Page<MemberGameDto> memberGameDtos = memberGames.map(MemberGameDto::new);
        return new RsData<>("200-1", "라이브러리 조회", memberGameDtos);
    }
    //라이브러리에 게임추가
    @PostMapping
    @Transactional
    @Operation(summary = "라이브러리에 멤버게임 추가")
    public RsData addToLibrary(@PathVariable("memberId") int memberId, @RequestBody MemberGameAddRequest request){
        if (memberId != rq.getActor().getId()){
            throw new ServiceException("403","Cannot add to this library");
        }
        Member actor = memberService.findById(rq.getActor().getId()).orElseThrow();
        Game game = gameService.findById(request.gameId).orElseThrow(() -> new ServiceException("404-1", "No Game"));
        MemberGame memberGame = memberGameService.addToLibrary(request.platform, request.playtime, request.isFavorite, request.status, actor, game);
        memberService.flush();
        return new RsData<>(
                "201-1",
                "라이브러리에 게임 %s가 추가되었습니다.".formatted(game.getName()),
                new MemberGameDto(memberGame)
        );
    }


    //게임정보 업데이트하기 (favorite, review, playtime 등)
    @PatchMapping("/{memberGameId}")
    @Transactional
    @Operation(summary = "라이브러리 게임 상태 업데이트")
    public RsData updateMemberGame(
            @PathVariable("memberId") int memberId,
            @PathVariable("memberGameId") int memberGameId,
            @Valid @RequestBody MemberGameUpdateRequest request
    ) {
        if (memberId != rq.getActor().getId()){
            throw new ServiceException("403","Cannot update this library");
        }
        MemberGame memberGame = memberGameService.updateMemberGame(
                memberGameId,
                memberId,
                request
        );

        return new RsData<>(
                "200",
                "게임 정보가 업데이트되었습니다.",
                new MemberGameDto(memberGame)
        );
    }


    @DeleteMapping("/{memberGameId}")
    @Transactional
    @Operation(summary = "라이브러리에서 게임삭제")
    public RsData<Void> removeFromLibrary(@PathVariable("memberId") int memberId, @PathVariable("memberGameId") int memberGameId){
        Member actor = rq.getActor();
        if (memberId != actor.getId()){
            throw new ServiceException("403","Cannot delete from this library");
        }
        Member member = Optional.ofNullable(memberService.findById(actor.getId())).orElseThrow();
        memberGameService.removeFromLibrary(member, memberGameId);
        memberService.flush();
        return new RsData<>(
                "204",
                "게임을 삭제하였습니다."
        );
    }
}