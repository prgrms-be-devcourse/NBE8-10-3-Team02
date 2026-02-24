package com.back.domain.review.dto;

import com.back.domain.review.entity.Review;

import java.time.LocalDateTime;


public record ReviewDto(
        int id,
        String title,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        int authorId,
        String authorNickName,
        int gameId,
        String gameName,
        String content,
        double rating
)
{
    public ReviewDto(Review review){
        this(
                review.getId(),
                review.getTitle(),
                review.getCreateDate(),
                review.getModifyDate(),
                review.getAuthor().getId(),
                review.getAuthor().getNickname(),
                review.getGame().getId(),
                review.getGame().getName(),
                review.getContent(),
                review.getRating()
        );
    }
}