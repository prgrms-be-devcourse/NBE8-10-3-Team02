package com.back.domain.post.dto;

import com.back.domain.post.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostDto (

    int id,
    int authorId,
    String authorName,
    String title,
    String content,
    LocalDateTime createDate,
    LocalDateTime modifyDate,
    List<String > tags,
    int viewCount,
    long likeCount
) {
    public PostDto(Post post) {
        this(
        post.getId(),
        post.getAuthor().getId(),
        post.getAuthor().getNickname(),
        post.getTitle(),
        post.getContent(),
        post.getCreateDate(),
        post.getModifyDate(),
                post.getPostTags().stream()
                        .map(pt->pt.getTag().getContent())
                        .toList(),
                post.getViewCount(),
                post.getPostLikes() != null ? post.getPostLikes().size() : 0
        );
    }
}