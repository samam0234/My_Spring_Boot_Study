package com.example.demo.repository;

import com.example.demo.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.author) LIKE LOWER(CONCAT('%', :q, '%'))
               OR EXISTS (
                    SELECT t.id FROM Tag t
                    WHERE t MEMBER OF p.tags
                      AND LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
               )
            """)
    Page<Post> search(@Param("q") String q, Pageable pageable);
}
