package com.example.demo.dto;

import com.example.demo.domain.Post;
import com.example.demo.domain.Tag;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tags
) {

    public static PostResponse from(Post post) {
        List<String> tagNames = post.getTags() == null
                ? List.of()
                : post.getTags().stream().map(Tag::getName).sorted().toList();
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                tagNames
        );
    }
}
