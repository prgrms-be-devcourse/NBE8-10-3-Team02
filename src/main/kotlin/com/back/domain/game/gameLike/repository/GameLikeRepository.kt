package com.back.domain.game.gameLike.repository

import com.back.domain.game.game.entity.Game
import com.back.domain.game.gameLike.entity.GameLike
import com.back.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface GameLikeRepository : JpaRepository<GameLike, Int> {
    // 특정 회원이 특정 게임에 좋아요 눌렀는지 확인
    fun findByMemberAndGame(
        member: Member,
        game: Game,
    ): Optional<GameLike>

    // 존재 여부만 확인 (성능 최적화)
    fun existsByMemberAndGame(
        member: Member,
        game: Game,
    ): Boolean

    // 특정 게임의 좋아요 수
    fun countByGame(game: Game): Long

    // 특정 회원이 좋아요 누른 게임 목록
    @Query("SELECT gl.game FROM GameLike gl WHERE gl.member = :member ORDER BY gl.createdAt DESC")
    fun findLikedGamesByMember(
        @Param("member") member: Member,
    ): List<Game>

    // igdbId로 좋아요 여부 확인 (조인 쿼리)
    @Query(
        "SELECT CASE WHEN COUNT(gl) > 0 THEN true ELSE false END " +
            "FROM GameLike gl WHERE gl.member.id = :memberId AND gl.game.igdbId = :igdbId",
    )
    fun existsByMemberIdAndGameIgdbId(
        @Param("memberId") memberId: Int,
        @Param("igdbId") igdbId: Long,
    ): Boolean

    @Query("SELECT gl.game.id FROM GameLike gl WHERE gl.member.id = :memberId")
    fun findGameIdsByMemberId(
        @Param("memberId") memberId: Int,
    ): List<Long>
}
