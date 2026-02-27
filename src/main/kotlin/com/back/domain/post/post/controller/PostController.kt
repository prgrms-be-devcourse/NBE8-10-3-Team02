package com.back.domain.post.post.controller

import com.back.domain.member.member.service.MemberService
import com.back.domain.post.dto.PostCreateRequest
import com.back.domain.post.dto.PostDto
import com.back.domain.post.dto.PostModifyRequest
import com.back.domain.post.post.service.PostService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService,
    private val memberService: MemberService,
    private val rq: Rq,
) {
    @GetMapping
    fun getItems(
        @RequestParam(value = "kw", defaultValue = "") kw: String,
        @RequestParam(value = "tag", defaultValue = "") tag: String,
        @PageableDefault(size = 10, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): RsData<Page<PostDto>> {
        val page = postService.search(kw, tag, pageable)
        val postDtos = page.map { PostDto(it) }

        return RsData("200-1", "게시글 목록 조회", postDtos)
    }

    @GetMapping("/{id}")
    fun getItem(
        @PathVariable id: Int,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): PostDto {
        val post =
            postService.findById(id)
                ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        postService.handlePostViewCount(id, request, response)

        return PostDto(post)
    }

    @PostMapping
    fun create(
        @RequestBody @Valid request: PostCreateRequest,
    ): RsData<PostDto> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")

        val author =
            memberService.findById(actor.id)
                ?: throw ServiceException("404-1", "회원 정보를 찾을 수 없습니다.")

        val post = postService.write(author, request.title, request.content, request.tags)

        return RsData("201-1", "게시글이 작성되었습니다.", PostDto(post))
    }

    @PutMapping("/{id}")
    fun modify(
        @PathVariable id: Int,
        @RequestBody @Valid request: PostModifyRequest,
    ): RsData<PostDto> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")

        val post =
            postService.findById(id)
                ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        val author =
            memberService.findById(actor.id)
                ?: throw ServiceException("404-1", "회원 정보를 찾을 수 없습니다.")

        postService.checkPermission(post, author)
        postService.modify(post, request)

        return RsData("200-1", "${id}번 게시글이 수정되었습니다.", PostDto(post))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Int,
    ): RsData<Unit> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")

        val post =
            postService.findById(id)
                ?: throw ServiceException("404-1", "해당 게시글을 찾을 수 없습니다.")

        val author =
            memberService.findById(actor.id)
                ?: throw ServiceException("404-1", "회원 정보를 찾을 수 없습니다.")

        postService.checkPermission(post, author)
        postService.delete(post)

        return RsData("200-1", "${id}번 글이 삭제되었습니다.")
    }

    @PostMapping("/{id}/like")
    fun toggleLike(
        @PathVariable id: Int,
    ): RsData<Long> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")

        val isLiked = postService.toggleLike(actor, id)
        val msg = if (isLiked) "좋아요를 눌렀습니다." else "좋아요를 취소했습니다."

        return RsData("200-1", msg, postService.getLikeCount(id))
    }
}
