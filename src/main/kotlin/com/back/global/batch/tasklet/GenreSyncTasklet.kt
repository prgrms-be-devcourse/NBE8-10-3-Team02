package com.back.global.batch.tasklet

import com.back.domain.game.game.entity.Genre
import com.back.domain.game.game.repository.GenreRepository
import com.back.global.igdb.BatchIgdbClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class GenreSyncTasklet(
    private val igdbClient: BatchIgdbClient,
    private val genreRepository: GenreRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val igdbGenres = igdbClient.fetchGenres()
        log.info("IGDB에서 장르 {}건 조회", igdbGenres.size)

        val igdbIds = igdbGenres.map { it.id }
        val existingMap = genreRepository.findByIgdbIdIn(igdbIds).associateBy { it.igdbId }

        var created = 0
        for (dto in igdbGenres) {
            if (!existingMap.containsKey(dto.id)) {
                genreRepository.save(Genre.createGenre(dto.id, dto.name))
                created++
            }
        }

        log.info("장르 동기화 완료: 신규 {}건, 기존 {}건 스킵", created, igdbGenres.size - created)
        return RepeatStatus.FINISHED
    }
}
