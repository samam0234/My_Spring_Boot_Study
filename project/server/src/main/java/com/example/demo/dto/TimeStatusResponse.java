package com.example.demo.dto;

public record TimeStatusResponse(
        String currentTime,
        String period,
        boolean night,
        String message
) {
}
