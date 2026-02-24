package com.back.domain.game.game.entity

import com.back.standard.util.TimeUt
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import lombok.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "game",
    uniqueConstraints = [UniqueConstraint(name = "uk_game_igdb_id", columnNames = "igdb_id")],
    indexes = [Index(name = "ix_game_name", columnList = "name")]
)
@JsonIgnoreProperties("hibernateLazyInitializer")
class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_seq")
    @SequenceGenerator(name = "game_seq", sequenceName = "game_id_seq", allocationSize = 50)
    @Setter(
        AccessLevel.PROTECTED
    )
    private var id = 0

    @Column(name = "igdb_id", nullable = false)
    private var igdbId: Long = 0

    private var name: String? = null

    @Column(nullable = false, columnDefinition = "TEXT")
    private var summary: String? = null

    @Column(columnDefinition = "TEXT")
    private var storyline: String? = null

    private var aggregatedRating: Double? = null

    private var franchiseIgdbId: Long? = null

    private var franchiseName: String? = null

    private var coverImageId: String? = null
    private var firstReleaseDate: LocalDate? = null
    private var lastFetchedAt: Instant? = null

    private var viewCount: Long = 0
    private var likeCount: Long = 0
    private var reviewCount: Long = 0


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
        this.name = name
        this.summary = summary
        this.coverImageId = coverImageId
        this.firstReleaseDate = TimeUt.epoch.toLocalDate(firstReleaseDateEpochSecond)
        this.lastFetchedAt = Instant.now()
        this.storyline = storyline
        this.aggregatedRating = aggregatedRating
        this.franchiseIgdbId = franchiseIgdbId
        this.franchiseName = franchiseName
    }

    fun incrementViewCount() {
        this.viewCount++
    }

    fun incrementLikeCount() {
        this.likeCount++
    }

    fun decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--
    }

    fun incrementReviewCount() {
        this.reviewCount++
    }

    fun decrementReviewCount() {
        if (this.reviewCount > 0) this.reviewCount--
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as Game
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    companion object {
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
        ): Game {
            val g = Game()
            g.igdbId = igdbId
            g.name = name
            g.summary = summary
            g.coverImageId = imageId
            g.firstReleaseDate = TimeUt.epoch.toLocalDate(firstReleaseDate)
            g.lastFetchedAt = Instant.now()
            g.storyline = storyline
            g.aggregatedRating = aggregatedRating
            g.franchiseIgdbId = franchiseIgdbId
            g.franchiseName = franchiseName

            return g
        }

        @JvmStatic
        fun createGame(
            igdbId: Long,
            name: String?,
            summary: String?,
            imageId: String?,
            firstReleaseDate: LocalDate?
        ): Game {
            val g = Game()
            g.igdbId = igdbId
            g.name = name
            g.summary = summary
            g.coverImageId = imageId
            g.firstReleaseDate = firstReleaseDate
            g.lastFetchedAt = Instant.now()

            return g
        }
    }
}
