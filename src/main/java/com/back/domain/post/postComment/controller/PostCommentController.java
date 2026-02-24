package com.back.domain.post.postComment.controller;


import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.PostCommentRepository;
import com.back.domain.post.postComment.dto.PostCommentCreateRequest;
import com.back.domain.post.postComment.dto.PostCommentDto;
import com.back.domain.post.postComment.dto.PostCommentModifyRequest;
import com.back.domain.post.postComment.entity.PostComment;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class PostCommentController {
    private final PostService postService;
    private final PostCommentRepository postCommentRepository;
    private final MemberService memberService;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    public List<PostCommentDto> getItems(
            @PathVariable int postId
    ) {
        Post post = postService.findById(postId)
                .orElseThrow(()-> new ServiceException("404-1", "해당 게시글을 찾을 수 없습니다."));

        return post
                .getComments()
                .stream()
                .map(PostCommentDto::new)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    public PostCommentDto getItem(
            @PathVariable int postId,
            @PathVariable int id
    ) {
        Post post = postService.findById(postId)
                .orElseThrow(()-> new ServiceException("404-1", "해당 게시글을 찾을 수 없습니다."));

        PostComment postComment = post.findCommentById(id)
                .orElseThrow(()-> new ServiceException("404-2", "해당 댓글을 찾을 수 없습니다."));

        return new PostCommentDto(postComment);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "삭제")
    public RsData<Void> delete(
            @PathVariable int postId,
            @PathVariable int id,
            @AuthenticationPrincipal SecurityUser user
    ) {

        Post post = postService.findById(postId)
                .orElseThrow(()-> new ServiceException("404-1", "해당 게시글을 찾을 수 없습니다."));

        PostComment postComment = postCommentRepository.findById(id)
                .orElseThrow(()-> new ServiceException("404-2", "해당 댓글을 찾을 수 없습니다."));

        if(postComment.getAuthor().getId() !=user.getId()){
            throw new ServiceException("403-1", "자신의 댓글만 삭제할 수 있습니다.");
        }

        postService.deleteComment(postComment);

        return new RsData<>(
                "200-1",
                "%d번 댓글이 삭제되었습니다.".formatted(id)
        );
    }



    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "수정")
    public RsData<Void> modify(
            @PathVariable int postId,
            @PathVariable int id,
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody PostCommentModifyRequest reqBody
    ) {

        Post post = postService.findById(postId)
                .orElseThrow(()-> new ServiceException("404-1", "해당 게시글을 찾을 수 없습니다."));
        PostComment postComment = post.findCommentById(id)
                .orElseThrow(()-> new ServiceException("404-2", "해당 댓글을 찾을 수 없습니다."));

        if(postComment.getAuthor().getId() != user.getId()){
            throw new ServiceException("403-1", "자신의 댓글만 수정할 수 있습니다.");
        }

        postService.modifyComment(postComment, reqBody.content());

        return new RsData<>(
                "200-1",
                "%d번 댓글이 수정되었습니다.".formatted(id)
        );
    }

    @PostMapping
    @Transactional
    @Operation(summary = "작성")
    public RsData<PostCommentDto> write(
            @PathVariable int postId,
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody PostCommentCreateRequest reqBody
    ) {
        if(user == null) throw new ServiceException("401-1", "로그인이 필요합니다.");
        Post post = postService.findById(postId)
                .orElseThrow(()-> new ServiceException("404-1", "해당 게시글을 찾을 수 없습니다."));

        Member author = Optional.ofNullable(memberService.findById(user.getId()))
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));
        PostComment postComment =
                postService.writeComment(author, post, reqBody.content(), reqBody.parentId());

        postService.flush();

        return new RsData<>(
                "201-1",
                "%d번 댓글이 작성되었습니다.".formatted(postComment.getId()),
                new PostCommentDto(postComment)
        );
    }


}
