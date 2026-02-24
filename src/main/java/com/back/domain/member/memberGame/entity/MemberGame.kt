package com.back.domain.member.memberGame.entity;

import com.back.domain.game.game.entity.Game;
import com.back.domain.game.platform.PlatformGroup;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.memberGame.StatusEnum;
import com.back.domain.review.entity.Review;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor
public class MemberGame extends BaseEntity {

    private Long platformId;
    private double playtime;
    private boolean isFavorite;

    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @ManyToOne(fetch = LAZY)
    private Member member;

    @ManyToOne(fetch = LAZY)
    private Game game;

    @OneToOne(fetch = LAZY)
    private Review review;

    public MemberGame(Long platformId, double playtime, boolean isFavorite, StatusEnum status, Member member, Game game) {
        this.platformId = platformId;
        this.playtime = playtime;
        this.isFavorite = isFavorite;
        this.status = status;
        this.member = member;
        this.game = game;
        this.review = null;
    }

    /**
     * Get the platform group name for display (e.g., "PC", "PS")
     */
    public String getPlatformGroupName() {
        return PlatformGroup.getGroupName(platformId);
    }
    public void checkActorCanAccess(Member actor) {
        if (!member.equals(actor))
            throw new ServiceException("403-1", "%d번 게임에 대한 권한이 없습니다.".formatted(getId()));
    }

    // Specific setters for mutable fields only
    public void setPlatformId(Long platformId) {
        this.platformId = platformId;
    }

    /**
     * Set platformId from group name (e.g., "PC" -> 6L)
     */
    public void setPlatformByGroupName(String groupName) {
        this.platformId = PlatformGroup.getDefaultPlatformId(groupName);
    }

    public void setPlaytime(double playtime) {
        this.playtime = playtime;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public void setReview(Review review) {
        this.review = review;
    }
}
