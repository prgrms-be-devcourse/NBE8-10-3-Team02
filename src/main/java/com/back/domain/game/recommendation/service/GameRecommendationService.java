package com.back.domain.game.recommendation.service;

import com.back.domain.game.recommendation.dto.GameRecommendationResponse;
import com.back.domain.game.recommendation.repository.GameVectorRepository;
import com.back.domain.game.recommendation.repository.MemberVectorRepository;
import com.back.domain.member.memberGame.repository.MemberGameRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRecommendationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int CANDIDATE_MULTIPLIER = 3;

    private final GameVectorRepository gameVectorRepository;
    private final MemberVectorRepository memberVectorRepository;
    private final MemberGameRepository memberGameRepository;

    @Transactional(readOnly = true)
    public List<GameRecommendationResponse> getPersonalRecommendations(int memberId, int limit) {
        String profileVector = memberVectorRepository.getProfileVectorString(memberId);
        if (profileVector == null || profileVector.isBlank()) {
            throw new ServiceException("404-1", "프로필 벡터가 아직 생성되지 않았습니다. 라이브러리에 게임을 추가해주세요.");
        }

        // 보유 게임 제외 목록
        List<Integer> ownedGameIds = memberGameRepository.findAllByMemberId(memberId)
                .stream()
                .map(mg -> mg.getGame().getId())
                .collect(Collectors.toList());

        if (ownedGameIds.isEmpty()) {
            ownedGameIds = List.of(-1); // native query IN clause에 빈 리스트 방지
        }

        int candidateLimit = Math.min(limit * CANDIDATE_MULTIPLIER, 100);
        List<Object[]> results = gameVectorRepository.findSimilarGamesByUserVector(
                profileVector, ownedGameIds, candidateLimit
        );

        return results.stream()
                .map(this::toRecommendationWithHybridScore)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GameRecommendationResponse> getSimilarGames(long igdbId, int limit) {
        List<Object[]> results = gameVectorRepository.findSimilarGamesByIgdbId(igdbId, limit);

        return results.stream()
                .map(this::toRecommendationWithHybridScoreForIgdb)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .collect(Collectors.toList());
    }

    /**
     * 하이브리드 스코어: 0.7×similarity + 0.2×(rating/100) + 0.1×(log(likeCount+1)/10)
     */
    private GameRecommendationResponse toRecommendationWithHybridScore(Object[] row) {
        int gameId = ((Number) row[0]).intValue();
        String name = (String) row[1];
        String coverImageId = (String) row[2];
        double rating = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        long likeCount = row[4] != null ? ((Number) row[4]).longValue() : 0;
        double similarity = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;

        double ratingScore = rating / 100.0;
        double popularityScore = Math.log1p(likeCount) / 10.0;
        double hybridScore = 0.7 * similarity + 0.2 * ratingScore + 0.1 * popularityScore;

        return new GameRecommendationResponse(gameId, name, coverImageId, hybridScore, similarity);
    }

    /**
     * igdb_id를 반환하는 유사 게임 쿼리용 매핑
     */
    private GameRecommendationResponse toRecommendationWithHybridScoreForIgdb(Object[] row) {
        int igdbId = ((Number) row[0]).intValue();
        String name = (String) row[1];
        String coverImageId = (String) row[2];
        double rating = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        long likeCount = row[4] != null ? ((Number) row[4]).longValue() : 0;
        double similarity = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;

        double ratingScore = rating / 100.0;
        double popularityScore = Math.log1p(likeCount) / 10.0;
        double hybridScore = 0.7 * similarity + 0.2 * ratingScore + 0.1 * popularityScore;

        return new GameRecommendationResponse(igdbId, name, coverImageId, hybridScore, similarity);
    }
}
