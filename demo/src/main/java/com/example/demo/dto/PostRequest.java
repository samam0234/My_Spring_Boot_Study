package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostRequest(

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @NotBlank(message = "작성자는 필수입니다.")
        String author,

        List<String> tags
) {
    public PostRequest(String title, String content, String author) {
        this(title, content, author, null);
    }
}
