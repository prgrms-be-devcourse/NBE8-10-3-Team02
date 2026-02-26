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

@Repository
class GameSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GameSearchRepositoryCustom {

    override fun searchByCondition(condition: GameSearchCondition): List<Game> {
        val builder = BooleanBuilder()

        // 1. 이름 검색 로직 분리 (!! 제거 및 가독성)
        condition.query?.takeIf { it.isNotBlank() }?.let { query ->
            val normalizedName = Expressions.stringTemplate("replace({0}, ' ', '')", game.name)
            builder.and(normalizedName.containsIgnoreCase(query.replace(" ", "")))
        }

        // 2. 장르 필터
        if (!condition.genreIds.isNullOrEmpty()) {
            builder.and(gameGenre.genre.igdbId.`in`(condition.genreIds))
        }

        // 3. 쿼리 생성 및 플랫폼 조인 전략
        return buildQuery(condition, builder).fetch()
    }

    private fun buildQuery(condition: GameSearchCondition, builder: BooleanBuilder) =
        queryFactory
            .selectDistinct(game)
            .from(game)
            .leftJoin(gameGenre).on(gameGenre.game.eq(game))
            .apply {
                if (!condition.platformIgdbIds.isNullOrEmpty()) {
                    join(gamePlatform).on(gamePlatform.game.eq(game))
                    builder.and(gamePlatform.platform.igdbId.`in`(condition.platformIgdbIds))
                } else {
                    leftJoin(gamePlatform).on(gamePlatform.game.eq(game))
                }
            }
            .where(builder)
}
