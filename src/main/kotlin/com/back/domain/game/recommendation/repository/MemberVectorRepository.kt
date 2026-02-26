package com.back.domain.game.recommendation.repository

import com.back.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberVectorRepository : JpaRepository<Member, Int> {
    @Modifying
    @Query(
        value = "UPDATE member SET profile_vector = cast(:vector as vector) WHERE id = :memberId",
        nativeQuery = true,
    )
    fun updateProfileVector(
        @Param("memberId") memberId: Int,
        @Param("vector") vector: String?,
    )

    @Query(
        value = "SELECT cast(profile_vector as text) FROM member WHERE id = :memberId",
        nativeQuery = true,
    )
    fun getProfileVectorString(
        @Param("memberId") memberId: Int,
    ): String?
}
