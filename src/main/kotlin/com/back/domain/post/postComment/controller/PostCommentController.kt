package com.back.domain.post.postComment.controller

import com.back.domain.member.member.service.MemberService
import com.back.domain.post.post.service.PostService
import com.back.domain.post.postComment.dto.PostCommentCreateRequest
import com.back.domain.post.postComment.dto.PostCommentDto
import com.back.domain.post.postComment.dto.PostCommentModifyRequest
import com.back.domain.post.postComment.repository.PostCommentRepository
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import com.back.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
class PostCommentController(
    private val postService: PostService,
    private val postCommentRepository: PostCommentRepository,
    private val memberService: MemberService
) {

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    fun getItems(@PathVariable postId: Int): List<PostCommentDto> {
        val post = postService.findById(postId)
            ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        return post.comments.map { PostCommentDto(it) }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    fun getItem(
        @PathVariable postId: Int,
        @PathVariable id: Int
    ): PostCommentDto {
        val post = postService.findById(postId)
            ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        val postComment = post.findCommentById(id)
            ?: throw ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.")

        return PostCommentDto(postComment)
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "삭제")
    fun delete(
        @PathVariable postId: Int,
        @PathVariable id: Int,
        @AuthenticationPrincipal user: SecurityUser
    ): RsData<Unit> {
        // postId 검증 (존재 여부 확인)
        postService.findById(postId)
            ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        val postComment = postCommentRepository.findById(id)
            .orElseThrow { ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.") }

        if (postComment.author.id != user.id) {
            throw ServiceException("403-1", "자신의 댓글만 삭제할 수 있습니다.")
        }

        postService.deleteComment(postComment)

        return RsData("200-1", "${id}번 댓글이 삭제되었습니다.")
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "수정")
    fun modify(
        @PathVariable postId: Int,
        @PathVariable id: Int,
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody reqBody: PostCommentModifyRequest
    ): RsData<Unit> {
        val post = postService.findById(postId)
            ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        val postComment = post.findCommentById(id)
            ?: throw ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.")

        if (postComment.author.id != user.id) {
            throw ServiceException("403-1", "자신의 댓글만 수정할 수 있습니다.")
        }

        postService.modifyComment(postComment, reqBody.content)

        return RsData("200-1", "${id}번 댓글이 수정되었습니다.")
    }

    @PostMapping
    @Transactional
    @Operation(summary = "작성")
    fun write(
        @PathVariable postId: Int,
        @AuthenticationPrincipal user: SecurityUser?,
        @Valid @RequestBody reqBody: PostCommentCreateRequest
    ): RsData<PostCommentDto> {
        val loginUser = user ?: throw ServiceException("401-1", "로그인이 필요합니다.")

        val post = postService.findById(postId)
            ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        val author = memberService.findById(loginUser.id)
            ?:throw ServiceException("404-1", "회원 정보를 찾을 수 없습니다.")

        val postComment = postService.writeComment(
            author, post, reqBody.content, reqBody.parentId
        )

        postService.flush()

        return RsData(
            "201-1",
            "${postComment.id}번 댓글이 작성되었습니다.",
            PostCommentDto(postComment)
        )
    }
}