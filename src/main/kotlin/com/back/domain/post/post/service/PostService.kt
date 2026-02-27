package com.back.domain.post.post.service

import com.back.domain.member.member.entity.Member
import com.back.domain.post.dto.PostModifyRequest
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostLike
import com.back.domain.post.post.repository.PostLikeRepository
import com.back.domain.post.post.repository.PostRepository
import com.back.domain.post.postComment.entity.PostComment
import com.back.domain.post.postComment.repository.PostCommentRepository
import com.back.domain.tag.tag.service.TagService
import com.back.global.exception.ServiceException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostService(
    private val postRepository: PostRepository,
    private val tagService: TagService,
    private val postCommentRepository: PostCommentRepository,
    private val postLikeRepository: PostLikeRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<Post> = postRepository.findAll(pageable)

    fun write(
        author: Member,
        title: String,
        content: String,
        tagNames: List<String>?,
    ): Post {
        val post = Post(author, title, content)
        val savedPost = postRepository.save(post)

        tagNames?.filter { it.isNotBlank() }?.forEach { tagName ->
            val tag = tagService.getOrCreate(tagName)
            savedPost.addTag(tag)
        }

        return savedPost
    }

    fun modify(
        post: Post,
        request: PostModifyRequest,
    ) {
        post.modify(request.title, request.content)
        post.postTags.clear()

        flush()

        request.tags.filter { it.isNotBlank() }.forEach { tagName ->
            val tag = tagService.getOrCreate(tagName)
            post.addTag(tag)
        }
    }

    fun findById(id: Int): Post? = postRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    fun search(
        kw: String?,
        tag: String?,
        pageable: Pageable,
    ): Page<Post> {
        val searchKw = if (!kw.isNullOrBlank()) kw else null
        val searchTag = if (!tag.isNullOrBlank()) tag else null

        if (searchKw == null && searchTag == null) {
            return postRepository.findAll(pageable)
        }

        return postRepository.search(searchKw, searchTag, pageable)
    }

    fun writeComment(
        author: Member,
        post: Post,
        content: String,
        parentCommentId: Int?,
    ): PostComment {
        val comment = PostComment(author, post, content)

        parentCommentId?.let { id ->
            val parent =
                postCommentRepository
                    .findById(id)
                    .orElseThrow { ServiceException("404-1", "댓글을 찾을 수 없습니다.") }

            if (parent.parent != null) {
                throw ServiceException("400-3", "대댓글에는 답글을 달 수 없습니다.")
            }
            comment.parent = parent
        }

        return postCommentRepository.save(comment)
    }

    fun deleteComment(postComment: PostComment) {
        if (postComment.children.isNotEmpty()) {
            postComment.markAsDeleted()
        } else {
            val parent = postComment.parent

            parent?.children?.remove(postComment)
            postCommentRepository.delete(postComment)

            if (parent != null && parent.deleted && parent.children.isEmpty()) {
                postCommentRepository.delete(parent)
            }
        }
    }

    fun modifyComment(
        postComment: PostComment,
        content: String,
    ) {
        postComment.modify(content)
    }

    fun delete(post: Post) {
        postRepository.delete(post)
    }

    fun flush() {
        postRepository.flush()
    }

    @Transactional(readOnly = true)
    fun checkPermission(
        post: Post,
        author: Member,
    ) {
        if (post.author.id != author.id) {
            throw ServiceException("403-1", "해당 게시글에 대한 권한이 없습니다.")
        }
    }

    fun toggleLike(
        member: Member,
        postId: Int,
    ): Boolean {
        val post =
            postRepository
                .findById(postId)
                .orElseThrow { ServiceException("404-1", "게시글이 존재하지 않습니다.") }

        val opLike = postLikeRepository.findByMemberAndPost(member, post)

        return if (opLike.isPresent) {
            postLikeRepository.delete(opLike.get())
            false
        } else {
            // PostLike.kt에서 만든 생성자 사용 (Builder 제거됨)
            postLikeRepository.save(PostLike(member = member, post = post))
            true
        }
    }

    @Transactional(readOnly = true)
    fun getLikeCount(postId: Int): Long {
        val post =
            postRepository
                .findById(postId)
                .orElseThrow { ServiceException("404-1", "게시글이 없습니다.") }

        return postLikeRepository.countByPost(post)
    }

    @Transactional
    fun increaseViewCount(id: Int) {
        val post = findById(id) ?: return
        post.increaseView()
    }

    fun handlePostViewCount(
        postId: Int,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        // 1. 모든 쿠키 중 "postView" 이름을 가진 쿠키를 찾음
        val cookies = request.cookies
        val viewCookie = cookies?.find { it.name == "postView" }

        // 2. 중복 체크 (쿠키 값 예: "[1][5][10]")
        val isAlreadyVisited = viewCookie?.value?.contains("[$postId]") ?: false

        if (!isAlreadyVisited) {
            // 중복이 아닐 때만 조회수 증가
            increaseViewCount(postId)

            // 3. 새 쿠키 값 생성
            val newValue = (viewCookie?.value ?: "") + "[$postId]"

            // 4. 쿠키 설정 (Path와 MaxAge가 매우 중요!)
            val newCookie =
                Cookie("postView", newValue).apply {
                    path = "/" // 서비스 전체에서 쿠키가 유지되도록 설정
                    maxAge = 60 * 60 * 24 // 24시간
                    isHttpOnly = true // 보안 설정
                    // secure = true        // HTTPS 환경이라면 추가
                }

            response.addCookie(newCookie)
            println("조회수 증가 완료: $postId, 현재 쿠키: $newValue") // 디버깅용 로그
        } else {
            println("이미 방문한 게시글입니다: $postId")
        }
    }
}
