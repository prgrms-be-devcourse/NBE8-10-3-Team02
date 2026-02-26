package com.back.domain.game.game.repository

import com.back.domain.game.game.entity.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByIgdbIdIn(igdbIds: Collection<Long>): List<Company>
}
