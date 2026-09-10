package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record WatchlistRequest(
        @NotBlank(message = "종목 코드는 필수입니다.")
        String symbol
) {
}
