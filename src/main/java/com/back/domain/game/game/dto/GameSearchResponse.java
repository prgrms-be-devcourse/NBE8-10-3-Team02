package com.back.domain.game.game.dto;

import com.back.global.igdb.dto.IgdbGameSummaryDto;
import com.back.standard.util.TimeUt;

import com.back.global.igdb.util.IgdbImageUtil;

import java.time.LocalDate;
import java.util.List;

import java.util.Map;
import java.util.Objects;


public record GameSearchResponse(
        long igdbId,
        String name,
        String imageUrl,
        LocalDate firstReleaseDate,
        List<String> genres,
        List<String> platforms
        // developerName 추가예정
) {
//    IGDB 조회용
    public static GameSearchResponse fromDto(
            IgdbGameSummaryDto d,
            Map<Long, String> genreMap,
            Map<Long, String> platformMap
    ) {
        return new GameSearchResponse(
                d.id(),
                d.name(),
                IgdbImageUtil.cover(
                        d.cover() != null ? d.cover().imageId() : null
                ),
                TimeUt.epoch.toLocalDate(d.firstReleaseDateEpochSeconds()),
                // 장르 null 체크
                d.genres() == null ? List.of() : d.genres().stream()
                        .map(genreMap::get)
                        .filter(Objects::nonNull)
                        .toList(),
                // 플랫폼 null 체크
                d.platforms() == null ? List.of() : d.platforms().stream()
                        .map(platformMap::get)
                        .filter(Objects::nonNull)
                        .toList()
        );
    }
}
