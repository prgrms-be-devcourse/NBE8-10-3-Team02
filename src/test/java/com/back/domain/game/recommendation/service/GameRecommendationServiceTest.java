package com.back.domain.game.recommendation.service;

import com.back.domain.game.game.entity.Game;
import com.back.domain.game.recommendation.dto.GameRecommendationResponse;
import com.back.domain.game.recommendation.repository.GameVectorRepository;
import com.back.domain.game.recommendation.repository.MemberVectorRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.memberGame.entity.MemberGame;
import com.back.domain.member.memberGame.repository.MemberGameRepository;
import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameRecommendationServiceTest {

    @Mock GameVectorRepository gameVectorRepository;
    @Mock MemberVectorRepository memberVectorRepository;
    @Mock MemberGameRepository memberGameRepository;

    @InjectMocks
    private GameRecommendationService service;

    private static final int MEMBER_ID = 1;

    @Nested
    @DisplayName("getPersonalRecommendations")
    class GetPersonalRecommendations {

        @Test
        @DisplayName("프로필 벡터가 null이면 ServiceException 발생")
        void nullProfileVector_throwsException() {
            when(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.getPersonalRecommendations(MEMBER_ID, 20))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("프로필 벡터가 아직 생성되지 않았습니다");
        }

        @Test
        @DisplayName("프로필 벡터가 빈 문자열이면 ServiceException 발생")
        void blankProfileVector_throwsException() {
            when(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("   ");

            assertThatThrownBy(() -> service.getPersonalRecommendations(MEMBER_ID, 20))
                    .isInstanceOf(ServiceException.class);
        }

        @Test
        @DisplayName("보유 게임이 없으면 excludeGameIds에 -1을 넣어 빈 IN clause 방지")
        void emptyLibrary_usesPlaceholderExcludeId() {
            when(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0,0.0]");
            when(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of());
            when(gameVectorRepository.findSimilarGamesByUserVector(anyString(), anyList(), anyInt()))
                    .thenReturn(List.of());

            service.getPersonalRecommendations(MEMBER_ID, 20);

            verify(gameVectorRepository).findSimilarGamesByUserVector(
                    eq("[1.0,0.0]"),
                    eq(List.of(-1)),
                    eq(60) // 20 × 3
            );
        }

        @Test
        @DisplayName("보유 게임 id들을 exclude 목록에 포함한다")
        void ownedGames_areExcluded() {
            Game game1 = Game.builder().id(10).igdbId(100).name("G1").summary("").build();
            Game game2 = Game.builder().id(20).igdbId(200).name("G2").summary("").build();
            Member member = new Member(1, "test@test.com", "tester");
            MemberGame mg1 = new MemberGame(1L, 0, false, null, member, game1);
            MemberGame mg2 = new MemberGame(1L, 0, false, null, member, game2);

            when(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0]");
            when(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of(mg1, mg2));
            when(gameVectorRepository.findSimilarGamesByUserVector(anyString(), anyList(), anyInt()))
                    .thenReturn(List.of());

            service.getPersonalRecommendations(MEMBER_ID, 10);

            verify(gameVectorRepository).findSimilarGamesByUserVector(
                    anyString(),
                    eq(List.of(10, 20)),
                    eq(30) // 10 × 3
            );
        }

        @Test
        @DisplayName("candidateLimit는 limit×3이고 최대 100이다")
        void candidateLimit_cappedAt100() {
            when(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0]");
            when(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of());
            when(gameVectorRepository.findSimilarGamesByUserVector(anyString(), anyList(), anyInt()))
                    .thenReturn(List.of());

            // limit=50 → 50×3=150 → min(150,100) = 100
            service.getPersonalRecommendations(MEMBER_ID, 50);

            verify(gameVectorRepository).findSimilarGamesByUserVector(
                    anyString(), anyList(), eq(100)
            );
        }

        @Test
        @DisplayName("하이브리드 스코어로 재정렬하여 limit개만 반환한다")
        void resortsAndLimits() {
            when(memberVectorRepository.getProfileVectorString(MEMBER_ID)).thenReturn("[1.0]");
            when(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of());

            // similarity는 낮지만 rating/likeCount가 높은 게임이 역전하는 시나리오
            // row: [id, name, coverImageId, rating, likeCount, similarity]
            List<Object[]> candidates = List.of(
                    row(1, "HighSim", null, 0.0, 0L, 0.95),    // 0.7×0.95 = 0.665
                    row(2, "HighRate", null, 100.0, 0L, 0.80),  // 0.7×0.80 + 0.2×1.0 = 0.76
                    row(3, "Popular", null, 50.0, 10000L, 0.70) // 0.7×0.70 + 0.2×0.5 + 0.1×log1p(10000)/10
            );
            when(gameVectorRepository.findSimilarGamesByUserVector(anyString(), anyList(), anyInt()))
                    .thenReturn(candidates);

            List<GameRecommendationResponse> results = service.getPersonalRecommendations(MEMBER_ID, 2);

            assertThat(results).hasSize(2);
            // 하이브리드 스코어 내림차순
            assertThat(results.get(0).score()).isGreaterThanOrEqualTo(results.get(1).score());
        }
    }

    @Nested
    @DisplayName("getSimilarGames")
    class GetSimilarGames {

        @Test
        @DisplayName("유사 게임 결과를 하이브리드 스코어 내림차순으로 반환한다")
        void returnsInHybridScoreOrder() {
            long igdbId = 999L;
            List<Object[]> rows = List.of(
                    row(1, "GameA", "cover1", 90.0, 500L, 0.85),
                    row(2, "GameB", "cover2", 60.0, 100L, 0.90)
            );
            when(gameVectorRepository.findSimilarGamesByIgdbId(igdbId, 10)).thenReturn(rows);

            List<GameRecommendationResponse> results = service.getSimilarGames(igdbId, 10);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).score()).isGreaterThanOrEqualTo(results.get(1).score());
        }

        @Test
        @DisplayName("결과가 비어있으면 빈 리스트를 반환한다")
        void emptyResults_returnsEmptyList() {
            when(gameVectorRepository.findSimilarGamesByIgdbId(999L, 10)).thenReturn(List.of());

            List<GameRecommendationResponse> results = service.getSimilarGames(999L, 10);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("하이브리드 스코어 계산")
    class HybridScoreCalculation {

        @Test
        @DisplayName("공식: 0.7×similarity + 0.2×(rating/100) + 0.1×(log1p(likeCount)/10)")
        void hybridScoreFormula() {
            double similarity = 0.85;
            double rating = 80.0;
            long likeCount = 100L;

            List<Object[]> rows = listOf(row(1, "Test", "cover", rating, likeCount, similarity));
            when(gameVectorRepository.findSimilarGamesByIgdbId(1L, 10)).thenReturn(rows);

            List<GameRecommendationResponse> results = service.getSimilarGames(1L, 10);

            double expected = 0.7 * similarity
                    + 0.2 * (rating / 100.0)
                    + 0.1 * (Math.log1p(likeCount) / 10.0);

            assertThat(results.get(0).score()).isCloseTo(expected, within(0.0001));
            assertThat(results.get(0).similarity()).isCloseTo(similarity, within(0.0001));
        }

        @Test
        @DisplayName("rating이 null이면 0으로 처리한다")
        void nullRating_treatedAsZero() {
            double similarity = 0.9;
            List<Object[]> rows = listOf(rowWithNulls(1, "Test", null, null, 0L, similarity));
            when(gameVectorRepository.findSimilarGamesByIgdbId(1L, 10)).thenReturn(rows);

            List<GameRecommendationResponse> results = service.getSimilarGames(1L, 10);

            double expected = 0.7 * similarity + 0.2 * 0.0 + 0.1 * (Math.log1p(0) / 10.0);
            assertThat(results.get(0).score()).isCloseTo(expected, within(0.0001));
        }

        @Test
        @DisplayName("likeCount가 null이면 0으로 처리한다")
        void nullLikeCount_treatedAsZero() {
            double similarity = 0.9;
            List<Object[]> rows = listOf(rowWithNulls(1, "Test", null, 80.0, null, similarity));
            when(gameVectorRepository.findSimilarGamesByIgdbId(1L, 10)).thenReturn(rows);

            List<GameRecommendationResponse> results = service.getSimilarGames(1L, 10);

            double expected = 0.7 * similarity + 0.2 * 0.8 + 0.1 * 0.0;
            assertThat(results.get(0).score()).isCloseTo(expected, within(0.0001));
        }

        @Test
        @DisplayName("similarity가 높을수록 하이브리드 스코어에 가장 큰 영향을 준다 (가중치 0.7)")
        void similarity_hasMostInfluence() {
            // 동일 rating/likeCount, similarity만 다른 2개
            List<Object[]> rows = List.of(
                    row(1, "HighSim", null, 50.0, 50L, 0.95),
                    row(2, "LowSim", null, 50.0, 50L, 0.30)
            );
            when(gameVectorRepository.findSimilarGamesByIgdbId(1L, 10)).thenReturn(rows);

            List<GameRecommendationResponse> results = service.getSimilarGames(1L, 10);

            // 하이브리드 스코어 차이의 대부분이 similarity에서 온다
            GameRecommendationResponse highSim = results.stream()
                    .filter(r -> r.name().equals("HighSim")).findFirst().orElseThrow();
            GameRecommendationResponse lowSim = results.stream()
                    .filter(r -> r.name().equals("LowSim")).findFirst().orElseThrow();
            double scoreDiff = highSim.score() - lowSim.score();
            double simContribDiff = 0.7 * (0.95 - 0.30);
            assertThat(scoreDiff).isCloseTo(simContribDiff, within(0.0001));
        }

        @Test
        @DisplayName("similarity 동점일 때 rating이 높으면 역전한다")
        void sameSimlarity_higherRatingWins() {
            List<Object[]> rows = List.of(
                    row(1, "LowRate", null, 20.0, 0L, 0.80),
                    row(2, "HighRate", null, 100.0, 0L, 0.80)
            );
            when(gameVectorRepository.findSimilarGamesByIgdbId(1L, 10)).thenReturn(rows);

            List<GameRecommendationResponse> results = service.getSimilarGames(1L, 10);

            assertThat(results.get(0).name()).isEqualTo("HighRate");
        }
    }

    // --- 팩토리 헬퍼 ---

    /**
     * native query 결과 row: [id, name, coverImageId, rating, likeCount, similarity]
     */
    private static Object[] row(int id, String name, String coverImageId,
                                 double rating, long likeCount, double similarity) {
        return new Object[]{id, name, coverImageId, rating, likeCount, similarity};
    }

    /**
     * rating/likeCount에 null을 넣을 수 있는 버전 (DB에서 null이 오는 케이스)
     */
    private static Object[] rowWithNulls(int id, String name, String coverImageId,
                                          Double rating, Long likeCount, double similarity) {
        return new Object[]{id, name, coverImageId, rating, likeCount, similarity};
    }

    /**
     * List.of()는 Object[] varargs를 분해하므로, 타입 안전한 래퍼
     */
    @SafeVarargs
    private static List<Object[]> listOf(Object[]... rows) {
        return Arrays.asList(rows);
    }
}
