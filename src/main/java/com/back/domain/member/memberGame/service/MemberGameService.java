package com.back.domain.member.memberGame.service;

import com.back.domain.game.game.entity.Game;
import com.back.domain.game.platform.PlatformGroup;
import com.back.domain.game.recommendation.event.ProfileVectorUpdateEvent;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.memberGame.StatusEnum;
import com.back.domain.member.memberGame.dto.MemberGameUpdateRequest;
import com.back.domain.member.memberGame.entity.MemberGame;
import com.back.domain.member.memberGame.repository.MemberGameRepository;
import com.back.domain.review.entity.Review;
import com.back.global.exception.ServiceException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberGameService {
    private final MemberRepository memberRepository;
    private final MemberGameRepository memberGameRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MemberGame addToLibrary(String platformGroupName, double playtime, boolean isFavorite, StatusEnum status, Member member, Game game) {
        // Check for duplicate game in library
        MemberGame existingGame = memberGameRepository.findByMemberIdAndGameId(member.getId(), game.getId());
        if (existingGame!= null) {
            throw new ServiceException("400-2", "이미 라이브러리에 존재하는 게임입니다.");
        }
        // Convert platform group name to platformId
        Long platformId = PlatformGroup.getDefaultPlatformId(platformGroupName);
        if (platformId == null) {
            throw new ServiceException("400-3", "유효하지 않은 플랫폼입니다: " + platformGroupName);
        }
        MemberGame memberGame = member.addMemberGame(platformId, playtime, isFavorite, status, game);
        eventPublisher.publishEvent(new ProfileVectorUpdateEvent(member.getId(), "addToLibrary"));
        return memberGame;
    }

    public boolean removeFromLibrary(Member member, int id) {
        boolean removed = member.removeGame(id);
        if (removed) {
            eventPublisher.publishEvent(new ProfileVectorUpdateEvent(member.getId(), "removeFromLibrary"));
        }
        return removed;
    }

    public MemberGame findByMemberAndGame(int memberId, int gameId) {
        return Optional.ofNullable(memberGameRepository.findByMemberIdAndGameId(memberId, gameId))
                .orElseThrow(() -> new ServiceException("404", "MemberGame not found"));
    }

    public Page<MemberGame> findByMemberId(int memberId, Pageable pageable) {
        return memberGameRepository.findByMemberId(memberId, pageable);
    }

    public Page<MemberGame> findByMemberIdWithFilters(int memberId, StatusEnum status, String platformGroupName, Pageable pageable) {
        List<Long> platformIds = platformGroupName != null ? PlatformGroup.getPlatformIds(platformGroupName) : null;

        if (status != null && platformIds != null && !platformIds.isEmpty()) {
            return memberGameRepository.findByMemberIdAndStatusAndPlatformIdIn(memberId, status, platformIds, pageable);
        } else if (status != null) {
            return memberGameRepository.findByMemberIdAndStatus(memberId, status, pageable);
        } else if (platformIds != null && !platformIds.isEmpty()) {
            return memberGameRepository.findByMemberIdAndPlatformIdIn(memberId, platformIds, pageable);
        } else {
            return memberGameRepository.findByMemberId(memberId, pageable);
        }
    }
    @Transactional
    public MemberGame updateMemberGame(int memberGameId, int memberId, @Valid MemberGameUpdateRequest request){
        MemberGame memberGame = memberGameRepository.findById(memberGameId)
                .orElseThrow(() -> new ServiceException("404", "Game not found"));
        // Verify ownership
        if (memberGame.getMember().getId() != memberId) {
            throw new ServiceException("403", "Not your game");
        }
        // Update fields
        if (request.getStatus() != null) memberGame.setStatus(request.getStatus());
        if (request.getPlaytime() != null) memberGame.setPlaytime(request.getPlaytime());
        if (request.isFavorite() != null) memberGame.setFavorite(request.isFavorite());
        if (request.getPlatform() != null) memberGame.setPlatformByGroupName(request.getPlatform());
        eventPublisher.publishEvent(new ProfileVectorUpdateEvent(memberId, "updateMemberGame"));
        return memberGame;
    }

    @Transactional
    public MemberGame updateReview(int memberId, int gameId, Review review){
        //This also verifies ownership of the game
        MemberGame memberGame = Optional.ofNullable(memberGameRepository.findByMemberIdAndGameId(memberId, gameId))
                .orElseThrow(() -> new ServiceException("404", "MemberGame not found"));
        memberGame.setReview(review);
        return memberGame;
    }


}