package com.back.domain.game.recommendation.service;

import com.back.domain.game.game.entity.Game;
import com.back.domain.game.gameLike.repository.GameLikeRepository;
import com.back.domain.game.recommendation.repository.GameVectorRepository;
import com.back.domain.game.recommendation.repository.MemberVectorRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.memberGame.StatusEnum;
import com.back.domain.member.memberGame.entity.MemberGame;
import com.back.domain.member.memberGame.repository.MemberGameRepository;
import com.back.domain.review.entity.Review;
import com.back.global.vector.VectorDimensionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.back.global.vector.VectorDimensionMapper.TOTAL_DIMENSIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileVectorServiceTest {

    @Mock MemberGameRepository memberGameRepository;
    @Mock GameLikeRepository gameLikeRepository;
    @Mock GameVectorRepository gameVectorRepository;
    @Mock MemberVectorRepository memberVectorRepository;

    @InjectMocks
    private UserProfileVectorService service;

    private static final int MEMBER_ID = 1;

    /*@BeforeEach
    void setUp() {
        service = new UserProfileVectorService(
                memberGameRepository, gameLikeRepository,
                gameVectorRepository, memberVectorRepository
        );
    }*/

    @Nested
    @DisplayName("calculateAndSaveProfileVector")
    class CalculateAndSaveProfileVector {

        @Test
        @DisplayName("라이브러리가 비어있으면 null 벡터를 저장한다")
        void emptyLibrary_savesNull() {
            when(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of());

            service.calculateAndSaveProfileVector(MEMBER_ID);

            verify(memberVectorRepository).updateProfileVector(MEMBER_ID, null);
            verify(gameVectorRepository, never()).findFeatureVectorsByMemberId(anyInt());

            verifyNoInteractions(gameLikeRepository);
            verifyNoMoreInteractions(memberVectorRepository, gameVectorRepository);
        }

        @Test
        @DisplayName("게임 1개만 보유 시 프로필 벡터 = 해당 게임 벡터")
        void singleGame_profileEqualsGameVector() {
            Game game = gameWithId(1);
            MemberGame mg = memberGame(game, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mg));
            stubFeatureVectors(new Object[]{1, vectorString(1.0f, 0.0f)});
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            // 가중 평균: 게임 1개이므로 weight / weight = 1 → 원본 벡터 그대로
            assertThat(saved[0]).isEqualTo(1.0f);
            assertThat(saved[1]).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("feature_vector가 없는 게임은 건너뛴다")
        void missingVector_isSkipped() {
            Game game1 = gameWithId(1);
            Game game2 = gameWithId(2);
            MemberGame mg1 = memberGame(game1, StatusEnum.PLAYING, false, 0, null);
            MemberGame mg2 = memberGame(game2, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mg1, mg2));
            // game1만 벡터 있음, game2는 없음
            stubFeatureVectors(new Object[]{1, vectorString(1.0f, 0.0f)});
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            assertThat(saved[0]).isEqualTo(1.0f);
            assertThat(saved[1]).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("모든 게임의 feature_vector가 없으면 영벡터를 저장한다")
        void allVectorsMissing_savesZeroVector() {
            Game game = gameWithId(1);
            MemberGame mg = memberGame(game, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mg));
            stubFeatureVectors(); // 빈 결과
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            for (float v : saved) {
                assertThat(v).isEqualTo(0.0f);
            }
        }
    }

    @Nested
    @DisplayName("가중치 계산 검증")
    class WeightCalculation {

        /**
         * 프로필 = Σ(vector × weight) / Σ(weight)
         * 두 게임의 벡터가 동일하면 V로 놓을 수 있고:
         *   = (V × w1 + V × w2) / (w1 + w2)
         *   = V × (w1 + w2) / (w1 + w2)
         *   = V
         */
        @Test
        @DisplayName("동일 벡터 2개 게임의 가중치가 다르면 결과는 동일 벡터")
        void sameVector_differentWeights_resultIsSameVector() {
            // 같은 벡터 × 다른 가중치 → 가중 평균 = 원본 벡터
            Game game1 = gameWithId(1);
            Game game2 = gameWithId(2);
            MemberGame mg1 = memberGame(game1, StatusEnum.COMPLETED, true, 100, null);  // 높은 가중치
            MemberGame mg2 = memberGame(game2, StatusEnum.DROPPED, false, 0, null);     // 낮은 가중치

            stubLibrary(List.of(mg1, mg2));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(1.0f, 0.0f)}
            );
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            assertThat(saved[0]).isEqualTo(1.0f);
            assertThat(saved[1]).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("서로 다른 벡터 2개 → 가중치 비율에 따라 가중 평균")
        void differentVectors_weightedAverage() {
            // gameA: vector=[1,0,...], weight = COMPLETED(2.0)
            // gameB: vector=[0,1,...], weight = DROPPED(0.5)
            // profile = [2.0/2.5, 0.5/2.5, ...] = [0.8, 0.2, ...]
            Game gameA = gameWithId(1);
            Game gameB = gameWithId(2);
            MemberGame mgA = memberGame(gameA, StatusEnum.COMPLETED, false, 0, null);
            MemberGame mgB = memberGame(gameB, StatusEnum.DROPPED, false, 0, null);

            stubLibrary(List.of(mgA, mgB));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(0.0f, 1.0f)}
            );
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            // weight A = 1.0 × 2.0 = 2.0, weight B = 1.0 × 0.5 = 0.5, total = 2.5
            assertThat(saved[0]).isCloseTo(0.8f, within(0.001f));
            assertThat(saved[1]).isCloseTo(0.2f, within(0.001f));
        }

        @Test
        @DisplayName("즐겨찾기 → 가중치에 2.5 곱셈")
        void favorite_multipliesWeight() {
            // gameA: favorite=true, PLAYING → 1.0 × 2.5 × 1.8 = 4.5
            // gameB: favorite=false, PLAYING → 1.0 × 1.8 = 1.8
            // total = 6.3, profile[0] = 4.5/6.3, profile[1] = 1.8/6.3
            Game gameA = gameWithId(1);
            Game gameB = gameWithId(2);
            MemberGame mgA = memberGame(gameA, StatusEnum.PLAYING, true, 0, null);
            MemberGame mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mgA, mgB));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(0.0f, 1.0f)}
            );
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            double wA = 2.5 * 1.8;  // 4.5
            double wB = 1.8;
            double total = wA + wB;  // 6.3
            assertThat(saved[0]).isCloseTo((float) (wA / total), within(0.001f));
            assertThat(saved[1]).isCloseTo((float) (wB / total), within(0.001f));
        }

        @ParameterizedTest
        @DisplayName("상태별 가중치 검증")
        @CsvSource({
                "COMPLETED, 2.0",
                "PLAYING, 1.8",
                "ON_HOLD, 1.2",
                "PLAN_TO_PLAY, 0.8",
                "DROPPED, 0.5"
        })
        void statusWeight(StatusEnum status, double expectedWeight) {
            // gameA: 해당 status → weight = expectedWeight
            // gameB: 기준점 (status=null 불가하므로, weight를 수식으로 검증)
            // 단일 게임이면 가중 평균 = 원본이라 가중치 차이를 볼 수 없음
            // → 2개 게임으로 비율 확인
            Game gameA = gameWithId(1);
            Game gameB = gameWithId(2);
            MemberGame mgA = memberGame(gameA, status, false, 0, null);
            MemberGame mgB = memberGame(gameB, StatusEnum.COMPLETED, false, 0, null); // weight=2.0 기준

            stubLibrary(List.of(mgA, mgB));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(0.0f, 1.0f)}
            );
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            double total = expectedWeight + 2.0;
            assertThat(saved[0]).isCloseTo((float) (expectedWeight / total), within(0.001f));
            assertThat(saved[1]).isCloseTo((float) (2.0 / total), within(0.001f));
        }

        @Test
        @DisplayName("플레이타임 > 0 → (1 + log1p(playtime) × 0.3) 가중치")
        void playtime_appliesLogWeight() {
            double playtime = 100.0;
            double playtimeWeight = 1.0 + Math.log1p(playtime) * 0.3;
            // 전체 weight = PLAYING(1.8) × playtimeWeight
            Game gameA = gameWithId(1);
            Game gameB = gameWithId(2);
            MemberGame mgA = memberGame(gameA, StatusEnum.PLAYING, false, playtime, null);
            MemberGame mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mgA, mgB));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(0.0f, 1.0f)}
            );
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            double wA = 1.8 * playtimeWeight;
            double wB = 1.8;
            double total = wA + wB;
            assertThat(saved[0]).isCloseTo((float) (wA / total), within(0.001f));
            assertThat(saved[1]).isCloseTo((float) (wB / total), within(0.001f));
        }

        @Test
        @DisplayName("좋아요한 게임 → 가중치에 1.3 곱셈")
        void likedGame_multipliesWeight() {
            Game gameA = gameWithId(1);
            Game gameB = gameWithId(2);
            MemberGame mgA = memberGame(gameA, StatusEnum.PLAYING, false, 0, null);
            MemberGame mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mgA, mgB));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(0.0f, 1.0f)}
            );
            // gameA(id=1)만 좋아요
            when(gameLikeRepository.findGameIdsByMemberId(MEMBER_ID)).thenReturn(List.of(1));

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            double wA = 1.8 * 1.3;  // PLAYING × liked
            double wB = 1.8;
            double total = wA + wB;
            assertThat(saved[0]).isCloseTo((float) (wA / total), within(0.001f));
            assertThat(saved[1]).isCloseTo((float) (wB / total), within(0.001f));
        }

        @Test
        @DisplayName("리뷰 평점 → 가중치에 (rating / 5.0) 곱셈")
        void reviewRating_multipliesWeight() {
            Game gameA = gameWithId(1);
            Game gameB = gameWithId(2);
            Review review = new Review("Good", "Great game", 4.0, null, gameA);
            MemberGame mgA = memberGame(gameA, StatusEnum.PLAYING, false, 0, review);
            MemberGame mgB = memberGame(gameB, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mgA, mgB));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(0.0f, 1.0f)}
            );
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            double wA = 1.8 * (4.0 / 5.0);  // PLAYING × review rating
            double wB = 1.8;
            double total = wA + wB;
            assertThat(saved[0]).isCloseTo((float) (wA / total), within(0.001f));
            assertThat(saved[1]).isCloseTo((float) (wB / total), within(0.001f));
        }

        @Test
        @DisplayName("리뷰 평점 0 → 평점 가중치 적용하지 않는다")
        void reviewRatingZero_noMultiplier() {
            Game gameA = gameWithId(1);
            Review review = new Review("Meh", "No score", 0.0, null, gameA);
            MemberGame mg = memberGame(gameA, StatusEnum.PLAYING, false, 0, review);

            stubLibrary(List.of(mg));
            stubFeatureVectors(new Object[]{1, vectorString(1.0f, 0.0f)});
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            // rating = 0 → if 조건 불통과 → 평점 가중치 미적용, 결과는 원본 벡터
            assertThat(saved[0]).isEqualTo(1.0f);
            assertThat(saved[1]).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("모든 가중치 복합 적용: 즐겨찾기 + COMPLETED + 플레이타임 + 좋아요 + 리뷰 vs 기본")
        void allWeightsCombined() {
            double playtime = 50.0;
            double rating = 4.5;

            // gameA: 모든 가중치 풀 적용
            Game gameA = gameWithId(1);
            Review review = new Review("Masterpiece", "Best ever", rating, null, gameA);
            MemberGame mgA = memberGame(gameA, StatusEnum.COMPLETED, true, playtime, review);

            // gameB: 기본 가중치만 (PLAN_TO_PLAY, 즐겨찾기 X, 플레이타임 0, 좋아요 X, 리뷰 X)
            Game gameB = gameWithId(2);
            MemberGame mgB = memberGame(gameB, StatusEnum.PLAN_TO_PLAY, false, 0, null);

            stubLibrary(List.of(mgA, mgB));
            stubFeatureVectors(
                    new Object[]{1, vectorString(1.0f, 0.0f)},
                    new Object[]{2, vectorString(0.0f, 1.0f)}
            );
            // gameA만 좋아요
            when(gameLikeRepository.findGameIdsByMemberId(MEMBER_ID)).thenReturn(List.of(1));

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();

            // gameA weight = 2.5(favorite) × 2.0(COMPLETED) × (1+log1p(50)×0.3)(playtime) × 1.3(liked) × (4.5/5.0)(review)
            double wA = 2.5 * 2.0
                    * (1.0 + Math.log1p(playtime) * 0.3)
                    * 1.3
                    * (rating / 5.0);
            // gameB weight = 0.8(PLAN_TO_PLAY)
            double wB = 0.8;
            double total = wA + wB;

            assertThat(saved[0]).isCloseTo((float) (wA / total), within(0.001f));
            assertThat(saved[1]).isCloseTo((float) (wB / total), within(0.001f));

            // gameA가 압도적으로 커야 한다 (90% 이상)
            assertThat(saved[0]).isGreaterThan(0.9f);
            assertThat(saved[1]).isLessThan(0.1f);
        }
    }

    @Nested
    @DisplayName("벡터 크기 검증")
    class VectorSize {

        @Test
        @DisplayName("저장되는 벡터는 항상 TOTAL_DIMENSIONS 차원이다")
        void savedVectorHasCorrectDimensions() {
            Game game = gameWithId(1);
            MemberGame mg = memberGame(game, StatusEnum.PLAYING, false, 0, null);

            stubLibrary(List.of(mg));
            stubFeatureVectors(new Object[]{1, fullDimensionVectorString()});
            stubLikedGameIds();

            service.calculateAndSaveProfileVector(MEMBER_ID);

            float[] saved = captureProfileVector();
            assertThat(saved).hasSize(TOTAL_DIMENSIONS);
        }
    }

    // --- stub 헬퍼 ---

    private void stubLibrary(List<MemberGame> memberGames) {
        when(memberGameRepository.findAllByMemberId(MEMBER_ID)).thenReturn(memberGames);
    }

    private void stubFeatureVectors(Object[]... rows) {
        when(gameVectorRepository.findFeatureVectorsByMemberId(MEMBER_ID))
                .thenReturn(List.of(rows));
    }

    private void stubLikedGameIds(Integer... gameIds) {
        when(gameLikeRepository.findGameIdsByMemberId(MEMBER_ID))
                .thenReturn(List.of(gameIds));
    }

    private float[] captureProfileVector() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(memberVectorRepository).updateProfileVector(eq(MEMBER_ID), captor.capture());
        return parseVector(captor.getValue());
    }

    // --- 팩토리 헬퍼 ---

    private static Game gameWithId(int id) {
        return Game.builder().id(id).igdbId(id * 100L).name("Game" + id).summary("").build();
    }

    private static MemberGame memberGame(Game game, StatusEnum status,
                                          boolean favorite, double playtime, Review review) {
        Member member = new Member(1, "test@test.com", "tester");
        MemberGame mg = new MemberGame(1L, playtime, favorite, status, member, game);
        if (review != null) {
            mg.setReview(review);
        }
        return mg;
    }

    /**
     * 처음 2개 차원만 값을 지정하고 나머지는 0.0인 pgvector 포맷 문자열 생성
     */
    private static String vectorString(float first, float second) {
        float[] v = new float[TOTAL_DIMENSIONS];
        v[0] = first;
        v[1] = second;
        return GameVectorService.vectorToString(v);
    }

    private static String fullDimensionVectorString() {
        float[] v = new float[TOTAL_DIMENSIONS];
        v[0] = 1.0f;
        v[TOTAL_DIMENSIONS - 1] = 0.5f;
        return GameVectorService.vectorToString(v);
    }

    private static float[] parseVector(String vectorStr) {
        String inner = vectorStr.substring(1, vectorStr.length() - 1);
        String[] parts = inner.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
