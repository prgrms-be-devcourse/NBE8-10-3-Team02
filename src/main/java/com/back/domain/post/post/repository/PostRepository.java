package com.back.domain.post.post.repository;

import com.back.domain.post.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Integer> {
//    Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
//    Page<Post> findByPostTags_Tag_Content(String content, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN p.postTags pt " +
            "LEFT JOIN pt.tag t " +
            "WHERE (:kw IS NULL OR p.title LIKE CONCAT('%', :kw, '%')) " +
            "AND (:tag IS NULL OR t.content LIKE CONCAT('%', :tag, '%'))")
    Page<Post> search(@Param("kw") String kw, @Param("tag") String tag, Pageable pageable);

}
