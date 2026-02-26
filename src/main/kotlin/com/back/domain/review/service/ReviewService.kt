package com.back.domain.review.service

import com.back.domain.game.game.entity.Game
import com.back.domain.game.game.repository.GameRepository
import com.back.domain.game.recommendation.event.ProfileVectorUpdateEvent
import com.back.domain.member.member.entity.Member
import com.back.domain.member.memberGame.repository.MemberGameRepository
import com.back.domain.review.entity.Review
import com.back.domain.review.repository.ReviewRepository
import com.back.global.exception.ServiceException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val gameRepository: GameRepository,
    private val memberGameRepository: MemberGameRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun findById(id: Int): Review? = reviewRepository.findById(id).orElse(null)

    fun findByAuthorAndGame(
        member: Member,
        game: Game,
    ): Review? = reviewRepository.findByAuthorAndGame(member, game)

    fun write(
        title: String,
        content: String,
        rating: Double,
        member: Member,
        game: Game,
    ): Review {
        memberGameRepository.findByMemberIdAndGameId(member.id, requireNotNull(game.id))
            ?: throw ServiceException("403-3", "라이브러리에 없는 게임은 리뷰를 작성할 수 없습니다.")

        if (reviewRepository.findByAuthorAndGame(member, game) != null) {
            throw ServiceException("400-1", "이미 해당 게임에 대한 리뷰를 작성하셨습니다.")
        }

        val review = reviewRepository.save(Review(title, content, rating, member, game))
        gameRepository.incrementReviewCount(requireNotNull(game.id))
        eventPublisher.publishEvent(ProfileVectorUpdateEvent(member.id, "writeReview"))
        return review
    }

    fun findAll(): List<Review> = reviewRepository.findAll()

    fun findAll(pageable: Pageable): Page<Review> = reviewRepository.findAll(pageable)

    fun findByAuthorId(authorId: Long): List<Review> = reviewRepository.findByAuthorId(authorId)

    fun findByAuthorId(
        authorId: Long,
        pageable: Pageable,
    ): Page<Review> = reviewRepository.findByAuthorId(authorId, pageable)

    fun findByGameId(gameId: Long): List<Review> = reviewRepository.findByGameId(gameId)

    fun findByGameId(
        gameId: Long,
        pageable: Pageable,
    ): Page<Review> = reviewRepository.findByGameId(gameId, pageable)

    fun modify(
        review: Review,
        title: String,
        content: String,
        rating: Double,
    ) {
        review.modify(title, content, rating)
        eventPublisher.publishEvent(ProfileVectorUpdateEvent(requireNotNull(review.author).id, "modifyReview"))
    }

    fun delete(review: Review) {
        val author = requireNotNull(review.author)
        val authorId = author.id
        gameRepository.decrementReviewCount(requireNotNull(review.game.id))
        memberGameRepository
            .findByMemberIdAndGameId(author.id, requireNotNull(review.game.id))
            ?.review = null
        reviewRepository.delete(review)
        eventPublisher.publishEvent(ProfileVectorUpdateEvent(authorId, "deleteReview"))
    }
}
