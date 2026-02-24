package com.back.domain.review.entity;

import com.back.domain.game.game.entity.Game;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Review extends BaseEntity {
    private String title;
    private String content;
    private double rating;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createDate;
    @LastModifiedDate
    private LocalDateTime modifyDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member author;

    public Review(String title, String content, double rating, Member author, Game game) {
        this.title = title;
        this.content = content;
        this.rating = rating;
        this.author = author;
        this.game = game;
    }

    public void modify(String title, String content, double rating) {
        this.title = title;
        this.content = content;
        this.rating = rating;
    }

    public void checkActorCanModify(Member actor) {
        if (author.getId() != actor.getId())
            throw new ServiceException("403-1", "%d번 리뷰 수정권한이 없습니다.".formatted(getId()));
    }

    public void checkActorCanDelete(Member actor) {
        if (author.getId() != actor.getId())
            throw new ServiceException("403-2", "%d번 리뷰 삭제권한이 없습니다.".formatted(getId()));
    }
}