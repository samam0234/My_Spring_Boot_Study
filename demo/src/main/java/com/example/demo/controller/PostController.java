package com.example.demo.controller;

import com.example.demo.domain.Post;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.PostRequest;
import com.example.demo.dto.PostResponse;
import com.example.demo.dto.PostUpdateRequest;
import com.example.demo.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest request) {
        Post saved = postService.create(request.title(), request.content(), request.author());
        return ResponseEntity.status(201).body(PostResponse.from(saved));
    }

    @GetMapping
    public PageResponse<PostResponse> findAll(@PageableDefault(size = 5) Pageable pageable) {
        return PageResponse.from(postService.findAll(pageable), PostResponse::from);
    }

    @GetMapping("/{id}")
    public PostResponse findById(@PathVariable Long id) {
        return PostResponse.from(postService.findById(id));
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostUpdateRequest request) {
        return PostResponse.from(postService.update(id, request.title(), request.content()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
