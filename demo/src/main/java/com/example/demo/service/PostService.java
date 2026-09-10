package com.example.demo.service;

import com.example.demo.domain.Post;
import com.example.demo.exception.PostNotFoundException;
import com.example.demo.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post create(String title, String content, String author) {
        return postRepository.save(new Post(title, content, author));
    }

    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    public Post update(Long id, String title, String content) {
        Post post = findById(id);
        post.update(title, content);
        return postRepository.save(post);
    }

    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new PostNotFoundException(id);
        }
        postRepository.deleteById(id);
    }
}
