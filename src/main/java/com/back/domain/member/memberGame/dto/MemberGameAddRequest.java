package com.back.domain.member.memberGame.dto;
import com.back.domain.member.memberGame.StatusEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberGameAddRequest(
        @NotBlank
        String platform,
        @Min(0)
        double playtime,
        boolean isFavorite,
        @NotNull
        StatusEnum status,
        int gameId
) {
}
