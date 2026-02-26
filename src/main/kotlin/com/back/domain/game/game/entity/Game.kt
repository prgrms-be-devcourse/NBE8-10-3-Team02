package com.back.domain.game.game.entity

import com.back.standard.util.TimeUt
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate
import java.util.Objects

@Entity
@JsonIgnoreProperties("hibernateLazyInitializer")
@Table(
    name = "game",
    uniqueConstraints = [UniqueConstraint(name = "uk_game_igdb_id", columnNames = ["igdb_id"])],
    indexes = [Index(name = "ix_game_name", columnList = "name")],
)
class Game protected constructor() {
    /**
     * 1. 의도 명시: id는 JPA SEQUENCE가 할당하는 값이므로 외부에서 직접 set하면 안 됨
     * 2. protected constructor()와 일관성: 생성자를 protected로 막아놨는데 setter가 public이면 반쪽짜리 보호
     * 3. JPA 동작에 무관: @Id 어노테이션이 필드에 있으므로 JPA는 리플렉션으로 필드에 직접 접근 → setter 가시성 무관
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_seq")
    @SequenceGenerator(name = "game_seq", sequenceName = "game_id_seq", allocationSize = 50)
    var id: Long? = null
        private set

    @Column(name = "igdb_id", nullable = false)
    var igdbId: Long = 0

    var name: String? = null

    @Column(nullable = false, columnDefinition = "TEXT")
    var summary: String = ""

    @Column(columnDefinition = "TEXT")
    var storyline: String? = null

    var aggregatedRating: Double? = null
    var franchiseIgdbId: Long? = null
    var franchiseName: String? = null
    var coverImageId: String? = null
    var firstReleaseDate: LocalDate? = null
    var lastFetchedAt: Instant? = null

    var viewCount: Long = 0
    var likeCount: Long = 0
    var reviewCount: Long = 0

    companion object {
        @JvmStatic
        fun createGame(
            igdbId: Long,
            name: String?,
            summary: String,
            imageId: String?,
            firstReleaseDate: Long?,
            storyline: String?,
            aggregatedRating: Double?,
            franchiseIgdbId: Long?,
            franchiseName: String?,
        ) = Game().apply {
            this.igdbId = igdbId
            this.name = name
            this.summary = summary
            this.coverImageId = imageId
            this.firstReleaseDate = TimeUt.epoch.toLocalDate(firstReleaseDate)
            this.lastFetchedAt = Instant.now()
            this.storyline = storyline
            this.aggregatedRating = aggregatedRating
            this.franchiseIgdbId = franchiseIgdbId
            this.franchiseName = franchiseName
        }

        @JvmStatic
        fun createGame(
            igdbId: Long,
            name: String?,
            summary: String,
            imageId: String?,
            firstReleaseDate: LocalDate?,
        ) = Game().apply {
            this.igdbId = igdbId
            this.name = name
            this.summary = summary
            this.coverImageId = imageId
            this.firstReleaseDate = firstReleaseDate
            this.lastFetchedAt = Instant.now()
        }
    }

    fun updateDetail(
        name: String?,
        summary: String,
        coverImageId: String?,
        firstReleaseDateEpochSecond: Long?,
    ) {
        this.name = name
        this.summary = summary
        this.coverImageId = coverImageId
        this.firstReleaseDate = TimeUt.epoch.toLocalDate(firstReleaseDateEpochSecond)
        this.lastFetchedAt = Instant.now()
    }

    fun updateDetail(
        name: String?,
        summary: String,
        coverImageId: String?,
        firstReleaseDateEpochSecond: Long?,
        storyline: String?,
        aggregatedRating: Double?,
        franchiseIgdbId: Long?,
        franchiseName: String?,
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
        viewCount++
    }

    fun incrementLikeCount() {
        likeCount++
    }

    fun decrementLikeCount() {
        if (likeCount > 0) likeCount--
    }

    fun incrementReviewCount() {
        reviewCount++
    }

    fun decrementReviewCount() {
        if (reviewCount > 0) reviewCount--
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return id == (other as Game).id
    }

    override fun hashCode() = Objects.hashCode(id)
}
