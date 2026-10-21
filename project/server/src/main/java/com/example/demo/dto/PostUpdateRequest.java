package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostUpdateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        List<String> tags
) {
    public PostUpdateRequest(String title, String content) {
        this(title, content, null);
    }
}
