package com.example.demo.dto;

public record WeatherResponse(
        String city,
        String country,
        double temperature,
        int humidity,
        double windSpeed,
        String description,
        String observedAt
) {
}
