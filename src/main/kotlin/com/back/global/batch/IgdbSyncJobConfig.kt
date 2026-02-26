package com.back.global.batch

import com.back.domain.game.game.repository.CompanyRepository
import com.back.domain.game.game.repository.GameCompanyRepository
import com.back.domain.game.game.repository.GameExternalIdRepository
import com.back.domain.game.game.repository.GameGameModeRepository
import com.back.domain.game.game.repository.GameGenreRepository
import com.back.domain.game.game.repository.GameKeywordRepository
import com.back.domain.game.game.repository.GameModeRepository
import com.back.domain.game.game.repository.GamePlatformRepository
import com.back.domain.game.game.repository.GamePlayerPerspectiveRepository
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.game.repository.GameThemeRepository
import com.back.domain.game.game.repository.GenreRepository
import com.back.domain.game.game.repository.KeywordRepository
import com.back.domain.game.game.repository.PlatformRepository
import com.back.domain.game.game.repository.PlayerPerspectiveRepository
import com.back.domain.game.game.repository.ThemeRepository
import com.back.domain.game.recommendation.repository.GameVectorRepository
import com.back.domain.game.recommendation.service.GameVectorService
import com.back.global.batch.dto.GameBatchItem
import com.back.global.batch.listener.DiscordBatchNotifier
import com.back.global.batch.processor.IgdbGameProcessor
import com.back.global.batch.reader.IgdbGamePageReader
import com.back.global.batch.tasklet.ApplyVectorStagingTasklet
import com.back.global.batch.tasklet.CompanySyncTasklet
import com.back.global.batch.tasklet.CreateVectorIndexTasklet
import com.back.global.batch.tasklet.DropVectorIndexTasklet
import com.back.global.batch.tasklet.GameModeSyncTasklet
import com.back.global.batch.tasklet.GenreSyncTasklet
import com.back.global.batch.tasklet.KeywordSyncTasklet
import com.back.global.batch.tasklet.PlatformSyncTasklet
import com.back.global.batch.tasklet.PlayerPerspectiveSyncTasklet
import com.back.global.batch.tasklet.ThemeSyncTasklet
import com.back.global.batch.writer.IgdbGameUpsertWriter
import com.back.global.igdb.BatchIgdbClient
import com.back.global.igdb.dto.IgdbGameDetailDto
import com.back.global.igdb.exception.IgdbApiException
import com.back.global.vector.VectorDimensionConfig
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * Job 하나와 Step 12개를 정의하는 설정 클래스
 * igdbSyncJob
 *     ├─  1) genreSyncStep              (Tasklet)
 *     ├─  2) platformSyncStep           (Tasklet)
 *     ├─  3) themeSyncStep              (Tasklet)
 *     ├─  4) gameModeSyncStep           (Tasklet)
 *     ├─  5) playerPerspectiveSyncStep  (Tasklet)
 *     ├─  6) keywordSyncStep            (Tasklet)
 *     ├─  7) companySyncStep            (Tasklet)
 *     ├─  8) vectorDimensionRefreshStep (Tasklet - 벡터 차원 매핑 1회 갱신)
 *     ├─  9) dropVectorIndexStep        (Tasklet - HNSW 인덱스 DROP)
 *     ├─ 10) gameSyncStep               (Chunk - 스테이징 테이블에 UPSERT)
 *     ├─ 11) applyVectorStagingStep     (Tasklet - staging → game 반영 + TRUNCATE, COMPLETED 시만)
 *     └─ 12) createVectorIndexStep      (Tasklet - HNSW 인덱스 CREATE, 항상 실행)
 */
@Configuration
class IgdbSyncJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val genreSyncTasklet: GenreSyncTasklet,
    private val platformSyncTasklet: PlatformSyncTasklet,
    private val themeSyncTasklet: ThemeSyncTasklet,
    private val gameModeSyncTasklet: GameModeSyncTasklet,
    private val playerPerspectiveSyncTasklet: PlayerPerspectiveSyncTasklet,
    private val keywordSyncTasklet: KeywordSyncTasklet,
    private val companySyncTasklet: CompanySyncTasklet,
    private val discordBatchNotifier: DiscordBatchNotifier,
    private val igdbClient: BatchIgdbClient,
    private val gameRepository: GameRepository,
    private val genreRepository: GenreRepository,
    private val platformRepository: PlatformRepository,
    private val themeRepository: ThemeRepository,
    private val gameModeRepository: GameModeRepository,
    private val playerPerspectiveRepository: PlayerPerspectiveRepository,
    private val keywordRepository: KeywordRepository,
    private val companyRepository: CompanyRepository,
    private val gameGenreRepository: GameGenreRepository,
    private val gamePlatformRepository: GamePlatformRepository,
    private val gameThemeRepository: GameThemeRepository,
    private val gameKeywordRepository: GameKeywordRepository,
    private val gameGameModeRepository: GameGameModeRepository,
    private val gamePlayerPerspectiveRepository: GamePlayerPerspectiveRepository,
    private val gameCompanyRepository: GameCompanyRepository,
    private val gameExternalIdRepository: GameExternalIdRepository,
    private val gameVectorRepository: GameVectorRepository,
    private val gameVectorService: GameVectorService,
    private val vectorDimensionRefresher: VectorDimensionConfig.VectorDimensionRefresher,
    private val applyVectorStagingTasklet: ApplyVectorStagingTasklet,
    private val dropVectorIndexTasklet: DropVectorIndexTasklet,
    private val createVectorIndexTasklet: CreateVectorIndexTasklet,
) {
    @Bean
    fun igdbSyncJob(): Job =
        JobBuilder("igdbSyncJob", jobRepository)
            .listener(discordBatchNotifier)
            .start(genreSyncStep())
            .next(platformSyncStep())
            .next(themeSyncStep())
            .next(gameModeSyncStep())
            .next(playerPerspectiveSyncStep())
            .next(keywordSyncStep())
            .next(companySyncStep())
            .next(vectorDimensionRefreshStep())
            .next(dropVectorIndexStep())
            .next(gameSyncStep())
            .on("COMPLETED")
            .to(applyVectorStagingStep())
            .next(createVectorIndexStep())
            .from(gameSyncStep())
            .on("*")
            .to(createVectorIndexStep())
            .end()
            .build()

    @Bean
    fun genreSyncStep(): Step =
        StepBuilder("genreSyncStep", jobRepository)
            .tasklet(genreSyncTasklet, transactionManager)
            .build()

    @Bean
    fun platformSyncStep(): Step =
        StepBuilder("platformSyncStep", jobRepository)
            .tasklet(platformSyncTasklet, transactionManager)
            .build()

    @Bean
    fun themeSyncStep(): Step =
        StepBuilder("themeSyncStep", jobRepository)
            .tasklet(themeSyncTasklet, transactionManager)
            .build()

    @Bean
    fun gameModeSyncStep(): Step =
        StepBuilder("gameModeSyncStep", jobRepository)
            .tasklet(gameModeSyncTasklet, transactionManager)
            .build()

    @Bean
    fun playerPerspectiveSyncStep(): Step =
        StepBuilder("playerPerspectiveSyncStep", jobRepository)
            .tasklet(playerPerspectiveSyncTasklet, transactionManager)
            .build()

    @Bean
    fun keywordSyncStep(): Step =
        StepBuilder("keywordSyncStep", jobRepository)
            .tasklet(keywordSyncTasklet, transactionManager)
            .build()

    @Bean
    fun companySyncStep(): Step =
        StepBuilder("companySyncStep", jobRepository)
            .tasklet(companySyncTasklet, transactionManager)
            .build()

    @Bean
    fun vectorDimensionRefreshStep(): Step =
        StepBuilder("vectorDimensionRefreshStep", jobRepository)
            .tasklet({ _, _ ->
                vectorDimensionRefresher.refresh()
                RepeatStatus.FINISHED
            }, transactionManager)
            .build()

    @Bean
    fun dropVectorIndexStep(): Step =
        StepBuilder("dropVectorIndexStep", jobRepository)
            .tasklet(dropVectorIndexTasklet, transactionManager)
            .build()

    @Bean
    fun createVectorIndexStep(): Step =
        StepBuilder("createVectorIndexStep", jobRepository)
            .tasklet(createVectorIndexTasklet, transactionManager)
            .build()

    @Bean
    fun applyVectorStagingStep(): Step =
        StepBuilder("applyVectorStagingStep", jobRepository)
            .tasklet(applyVectorStagingTasklet, transactionManager)
            .build()

    /**
     * 빨간줄은 Spring Batch 6.0 정식 릴리즈 전까지는 대체 API가 아직 안정화되지 않았기 때문에, 지금은 그대로 두는 게 낫다.
     */
    @Bean
    fun gameSyncStep(): Step =
        StepBuilder("gameSyncStep", jobRepository)
            .chunk<IgdbGameDetailDto, GameBatchItem>(500, transactionManager)
            .reader(igdbGamePageReader())
            .processor(igdbGameProcessor())
            .writer(igdbGameUpsertWriter())
            .faultTolerant()
            .skip(IgdbApiException::class.java)
            .skipLimit(100) // 최대 100건까지 오류 허용, 101번째 실패 시 step이 실패
            .build()

    @Bean
    @StepScope // step 실행 시마다 새 인스턴스가 생성됨
    fun igdbGamePageReader(): IgdbGamePageReader {
        // DB에 마지막 동기화 시점이 있으면 그 이후 변경분만 조회 (증분 동기화)
        // 없으면 null → 전체 동기화 (최초 실행)
        val updatedAfterEpoch =
            gameRepository
                .findMaxLastFetchedAt()
                .map { it.epochSecond }
                .orElse(null)
        return IgdbGamePageReader(igdbClient, updatedAfterEpoch)
    }

    @Bean
    @StepScope
    fun igdbGameProcessor(): IgdbGameProcessor =
        IgdbGameProcessor(
            genreRepository,
            platformRepository,
            themeRepository,
            gameModeRepository,
            playerPerspectiveRepository,
            keywordRepository,
            companyRepository,
        )

    // writer는 상태가 없어서 싱글턴으로 충분함
    @Bean
    fun igdbGameUpsertWriter(): IgdbGameUpsertWriter =
        IgdbGameUpsertWriter(
            gameRepository,
            gameGenreRepository,
            gamePlatformRepository,
            gameThemeRepository,
            gameKeywordRepository,
            gameGameModeRepository,
            gamePlayerPerspectiveRepository,
            gameCompanyRepository,
            gameExternalIdRepository,
            gameVectorRepository,
            gameVectorService,
        )
}
