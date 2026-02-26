package com.back.domain.game.game.service;

import com.back.domain.game.game.dto.GameSearchCondition;

import com.back.domain.game.game.dto.GameSearchResponse;
import com.back.domain.game.game.entity.Genre;
import com.back.domain.game.game.repository.GenreRepository;
import com.back.global.exception.ServiceException;
import com.back.global.igdb.dto.IgdbGameSummaryDto;
import com.back.global.igdb.service.IgdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.back.domain.game.platform.PlatformGroup.PLATFORM_MAP;
import static com.back.global.search.SearchNormalizer.normalize;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameSearchService {

    private final IgdbService igdbService;
    private final GenreRepository genreRepository;


    public List<GameSearchResponse> search(GameSearchCondition condition) {

        // 1. 입력 값 정규화 및 검증
        if (condition.getQuery() == null || condition.getQuery().isBlank()) {
            throw new ServiceException("400-2", "검색어를 입력해주세요.");
        }
        condition.setQuery(normalize(condition.getQuery()));

        boolean hasPlatformFilter =
                condition.getPlatformCode() != null &&
                        !condition.getPlatformCode().isBlank();

        if (hasPlatformFilter) {
            String normalized = condition.getPlatformCode().trim().toUpperCase();
            List<Long> mappedIds = PLATFORM_MAP.get(normalized);

            if (mappedIds == null || mappedIds.isEmpty()) {
                throw new ServiceException("400-1", "지원하지 않는 플랫폼 코드입니다: " + normalized);
            }

            condition.setPlatformIgdbIds(mappedIds);
        } else {
            condition.setPlatformIgdbIds(null);
        }


//       DB검색 로직 (추후 확장예정)
//        List<Game> games =
//                gameSearchRepository.searchByCondition(condition);
//
//        if (!games.isEmpty()) {
//            return games.stream()
//                    .map(GameSearchResponse::from)
//                    .toList();
//        }

//        IGDB 검색
        List<IgdbGameSummaryDto> igdbGames;
        try {
            igdbGames = igdbService.search(condition);
        } catch (Exception e) {
            throw new ServiceException("502-1", "외부 게임 검색 서버 오류");
        }

        // 2. 장르 매핑
        Set<Long> genreIgdbIds = igdbGames.stream()
                .filter(g -> g.getGenres() != null)
                .flatMap(g -> g.getGenres().stream())
                .collect(Collectors.toSet());

        Map<Long, String> genreMap =
                genreRepository.findByIgdbIdIn(genreIgdbIds).stream()
                        .collect(Collectors.toMap(
                                Genre::getIgdbId,
                                Genre::getName
                        ));


//        플랫폼 수집
        Set<Long> platformIds = igdbGames.stream()
                .filter(g -> g.getPlatforms() != null)
                .flatMap(g -> g.getPlatforms().stream())
                .collect(Collectors.toSet());

        Map<Long, String> platformMap =
                igdbService.getPlatformNameMap(platformIds);



        // 4. DTO 변환
        return igdbGames.stream()
                .map(d -> GameSearchResponse.fromDto(d, genreMap, platformMap))
                .toList();

    }


}
