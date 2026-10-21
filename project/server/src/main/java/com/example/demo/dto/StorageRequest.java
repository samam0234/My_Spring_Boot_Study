package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record StorageRequest(
        @NotBlank(message = "저장할 값은 필수입니다.")
        String value
) {
}
