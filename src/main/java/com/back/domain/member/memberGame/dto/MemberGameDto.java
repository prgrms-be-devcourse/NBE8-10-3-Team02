package com.back.domain.member.memberGame.dto;

import com.back.domain.member.memberGame.StatusEnum;
import com.back.domain.member.memberGame.entity.MemberGame;

public record MemberGameDto(
        int id,
        String platform,
        double playtime,
        boolean isFavorite,
        StatusEnum status,
        Long gameId,
        Long igdbId,
        String gameName,
        String coverImageId,
        Integer reviewId,
        Double rating
){
    public MemberGameDto(MemberGame memberGame){
        this(
                memberGame.getId(),
                memberGame.getPlatformGroupName(),
                memberGame.getPlaytime(),
                memberGame.isFavorite(),
                memberGame.getStatus(),
                memberGame.getGame().getId(),
                memberGame.getGame().getIgdbId(),
                memberGame.getGame().getName(),
                memberGame.getGame().getCoverImageId(),
                memberGame.getReview() != null ? memberGame.getReview().getId() : null,
                memberGame.getReview() != null ? memberGame.getReview().getRating() : null
        );
    }
}