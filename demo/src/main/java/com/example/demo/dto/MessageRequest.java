package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(

        @NotBlank(message = "받는 사람은 필수입니다.")
        String receiverUsername,

        @NotBlank(message = "내용은 필수입니다.")
        String content
) {
}
