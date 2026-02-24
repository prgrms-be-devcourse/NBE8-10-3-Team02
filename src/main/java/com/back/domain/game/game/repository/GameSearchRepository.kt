package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Game
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 게임 검색을 위한 리포지토리 인터페이스입니다.
 * JpaRepository와 Querydsl용 Custom 리포지토리를 상속받습니다.
 */
interface GameSearchRepository : JpaRepository<Game, Long>, GameSearchRepositoryCustom