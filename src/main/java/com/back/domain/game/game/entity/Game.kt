package com.back.domain.game.game.entity

import com.back.standard.util.TimeUt
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(
    name = "game",
    uniqueConstraints = [UniqueConstraint(name = "uk_game_igdb_id", columnNames = ["igdb_id"])],
    indexes = [Index(name = "ix_game_name", columnList = "name")]
)
@JsonIgnoreProperties("hibernateLazyInitializer")
class Game(
    @Column(name = "igdb_id", nullable = false)
    var igdbId: Long = 0,

    var name: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(columnDefinition = "TEXT")
    var storyline: String? = null,

    var aggregatedRating: Double? = null,

    var franchiseIgdbId: Long? = null,

    var franchiseName: String? = null,

    var coverImageId: String? = null,
    var firstReleaseDate: LocalDate? = null,
    var lastFetchedAt: Instant? = null,

    var viewCount: Long = 0,
    var likeCount: Long = 0,
    var reviewCount: Long = 0
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_seq")
    @SequenceGenerator(name = "game_seq", sequenceName = "game_id_seq", allocationSize = 50)
    var id: Int = 0
        protected set


    fun updateDetail(name: String?, summary: String?, coverImageId: String?, firstReleaseDateEpochSecond: Long?) {
        this.name = name
        this.summary = summary
        this.coverImageId = coverImageId
        this.firstReleaseDate = TimeUt.epoch.toLocalDate(firstReleaseDateEpochSecond)
        this.lastFetchedAt = Instant.now()
    }

    fun updateDetail(
        name: String?, summary: String?, coverImageId: String?, firstReleaseDateEpochSecond: Long?,
        storyline: String?, aggregatedRating: Double?, franchiseIgdbId: Long?, franchiseName: String?
    ) {
        updateDetail(name, summary, coverImageId, firstReleaseDateEpochSecond)
        this.storyline = storyline
        this.aggregatedRating = aggregatedRating
        this.franchiseIgdbId = franchiseIgdbId
        this.franchiseName = franchiseName
    }

    fun incrementViewCount() { this.viewCount++ }
    fun incrementLikeCount() { this.likeCount++ }
    fun decrementLikeCount() { if (this.likeCount > 0) this.likeCount-- }
    fun incrementReviewCount() { this.reviewCount++ }
    fun decrementReviewCount() { if (this.reviewCount > 0) this.reviewCount-- }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Game) return false
        return id != 0 && id == other.id
    }

    override fun hashCode(): Int = Objects.hashCode(id)

    companion object {
        // 기존 빌더 패턴 대신 사용할 정적 팩토리 메서드들
        @JvmStatic
        fun createGame(
            igdbId: Long,
            name: String?,
            summary: String?,
            imageId: String?,
            firstReleaseDate: Long?,
            storyline: String?,
            aggregatedRating: Double?,
            franchiseIgdbId: Long?,
            franchiseName: String?
        ): Game = Game(
            igdbId = igdbId,
            name = name,
            summary = summary,
            coverImageId = imageId,
            firstReleaseDate = TimeUt.epoch.toLocalDate(firstReleaseDate),
            storyline = storyline,
            aggregatedRating = aggregatedRating,
            franchiseIgdbId = franchiseIgdbId,
            franchiseName = franchiseName,
            lastFetchedAt = Instant.now()
        )

        @JvmStatic
        fun createGame(
            igdbId: Long,
            name: String?,
            summary: String?,
            imageId: String?,
            firstReleaseDate: LocalDate?
        ): Game = Game(
            igdbId = igdbId,
            name = name,
            summary = summary,
            coverImageId = imageId,
            firstReleaseDate = firstReleaseDate,
            lastFetchedAt = Instant.now()
        )
    }
}