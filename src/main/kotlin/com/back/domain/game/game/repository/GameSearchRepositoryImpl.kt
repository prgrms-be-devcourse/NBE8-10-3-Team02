package com.back.domain.game.game.repository

import com.back.domain.game.game.dto.GameSearchCondition
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.entity.QGame.game
import com.back.domain.game.game.entity.QGameGenre.gameGenre
import com.back.domain.game.game.entity.QGamePlatform.gamePlatform
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

// 1. @RequiredArgsConstructor 대신 생성자에 직접 선언해야 주입이 됩니다.
@Repository
class GameSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GameSearchRepositoryCustom {
    override fun searchByCondition(condition: GameSearchCondition): List<Game> {
        val builder = BooleanBuilder()

        // 1. 게임 이름 자연어 검색
        if (!condition.query.isNullOrBlank()) {
            val normalizedName =
                Expressions.stringTemplate(
                    "replace({0}, ' ', '')",
                    game.name,
                )
            // 검색어에서도 공백을 제거하고 비교해야 정합성이 맞습니다.
            builder.and(normalizedName.containsIgnoreCase(condition.query!!.replace(" ", "")))
        }

        // 2. 장르 필터
        if (!condition.genreIds.isNullOrEmpty()) {
            builder.and(gameGenre.genre.igdbId.`in`(condition.genreIds))
        }

        val hasPlatform = !condition.platformIgdbIds.isNullOrEmpty()

        val query =
            queryFactory
                .selectDistinct(game)
                .from(game)
                .leftJoin(gameGenre)
                .on(gameGenre.game.eq(game))

        // 3. 플랫폼 필터 조인 전략
        if (hasPlatform) {
            query.join(gamePlatform).on(gamePlatform.game.eq(game))
            builder.and(gamePlatform.platform.igdbId.`in`(condition.platformIgdbIds))
        } else {
            query.leftJoin(gamePlatform).on(gamePlatform.game.eq(game))
        }

        return query
            .where(builder)
            .fetch()
    }
}
