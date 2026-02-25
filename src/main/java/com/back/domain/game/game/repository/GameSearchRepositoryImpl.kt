package com.back.domain.game.game.repository

import com.back.domain.game.game.dto.GameSearchCondition
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.entity.QGame.game
import com.back.domain.game.game.entity.QGameGenre.gameGenre
import com.back.domain.game.game.entity.QGamePlatform.gamePlatform
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class GameSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GameSearchRepositoryCustom {
    override fun searchByCondition(condition: GameSearchCondition): List<Game> {
        // null 체크와 타입 변환을 동시에 처리
        val platformIds = condition.platformIgdbIds?.filterNotNull()
        val hasPlatform = !platformIds.isNullOrEmpty()

        val query =
            queryFactory
                .selectDistinct(game)
                .from(game)
                .leftJoin(gameGenre)
                .on(gameGenre.game.eq(game))

        if (hasPlatform) {
            query.join(gamePlatform).on(gamePlatform.game.eq(game))
        } else {
            query.leftJoin(gamePlatform).on(gamePlatform.game.eq(game))
        }

        return query
            .where(
                nameContains(condition.query),
                genreIn(condition.genreIds?.filterNotNull()),
                platformIn(platformIds),
            ).fetch()
    }

    private fun nameContains(query: String?): BooleanExpression? {
        if (query.isNullOrBlank()) return null
        return Expressions
            .stringTemplate("replace({0}, ' ', '')", game.name)
            .containsIgnoreCase(query.replace(" ", ""))
    }

    private fun genreIn(genreIds: List<Long>?): BooleanExpression? =
        if (genreIds.isNullOrEmpty()) {
            null
        } else {
            gameGenre.genre.igdbId.`in`(genreIds)
        }

    private fun platformIn(platformIds: List<Long>?): BooleanExpression? =
        if (platformIds.isNullOrEmpty()) {
            null
        } else {
            gamePlatform.platform.igdbId.`in`(platformIds)
        }
}
