package com.back.domain.post.post.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.postComment.entity.PostComment;
import com.back.domain.tag.postTag.entity.PostTag;
import com.back.domain.tag.tag.entity.Tag;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor
public class Post extends BaseEntity {
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createDate;
    @LastModifiedDate
    private LocalDateTime modifyDate;

    @ManyToOne(fetch = LAZY)
    private Member author;

    @OneToMany(mappedBy = "post", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

    @Column(columnDefinition = "integer default 0", nullable = false)
    private int viewCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikes = new ArrayList<>();

    public Post(Member author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public static Post create(Member author, String title, String content) {
        return new Post(author, title, content);
    }

    public void modify(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public PostComment addComment(Member author, String content) {
        PostComment postComment = new PostComment(author,this, content);
        comments.add(postComment);

        return postComment;
    }

    public Optional<PostComment> findCommentById(int id) {
        return comments
                .stream()
                .filter(comment -> comment.getId() == id)
                .findFirst();
    }

    public boolean deleteComment(PostComment postComment) {
        if (postComment == null) return false;

        return comments.remove(postComment);
    }

    public void addTag(Tag tag){
        PostTag postTag = new PostTag(this, tag);
        this.postTags.add(postTag);

    }

    public void increaseViewCount() {
        this.viewCount++;
    }

//    public boolean deleteTag(Tag tag){
//        return postTags.removeIf(
//                postTag ->postTag.getTag().equals(tag)
//        );
//    }

}
