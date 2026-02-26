package com.back.global.batch.dto

import com.back.domain.game.game.entity.Company
import com.back.domain.game.game.entity.CompanyRole
import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.entity.GameMode
import com.back.domain.game.game.entity.Genre
import com.back.domain.game.game.entity.Keyword
import com.back.domain.game.game.entity.Platform
import com.back.domain.game.game.entity.PlayerPerspective
import com.back.domain.game.game.entity.Theme

data class GameBatchItem(
    val game: Game,
    val genres: List<Genre>,
    val platforms: List<Platform>,
    val themes: List<Theme>,
    val keywords: List<Keyword>,
    val gameModes: List<GameMode>,
    val playerPerspectives: List<PlayerPerspective>,
    val companies: List<CompanyRoleEntry>,
    val externalIds: List<ExternalIdEntry>,
) {
    data class CompanyRoleEntry(
        val company: Company,
        val role: CompanyRole,
    )

    data class ExternalIdEntry(
        val platform: String,
        val externalId: String,
    )
}
