package com.example.demo.service;

import com.example.demo.domain.Post;
import com.example.demo.domain.Tag;
import com.example.demo.exception.PostNotFoundException;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;

    public PostService(PostRepository postRepository, TagRepository tagRepository) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
    }

    public Post create(String title, String content, String author, List<String> tagNames) {
        Post post = new Post(title, content, author);
        post.replaceTags(resolveTags(tagNames));
        return postRepository.save(post);
    }

    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<Post> search(String keyword, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.search(keyword, sorted);
    }

    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    public Post update(Long id, String title, String content, List<String> tagNames) {
        Post post = findById(id);
        post.update(title, content);
        if (tagNames != null) {
            post.replaceTags(resolveTags(tagNames));
        }
        return postRepository.save(post);
    }

    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new PostNotFoundException(id);
        }
        postRepository.deleteById(id);
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        Set<Tag> tags = new LinkedHashSet<>();
        if (tagNames == null) {
            return tags;
        }
        for (String raw : tagNames) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.length() > 40) {
                name = name.substring(0, 40);
            }
            String savedName = name;
            tags.add(tagRepository.findByName(savedName)
                    .orElseGet(() -> tagRepository.save(new Tag(savedName))));
        }
        return tags;
    }
}
