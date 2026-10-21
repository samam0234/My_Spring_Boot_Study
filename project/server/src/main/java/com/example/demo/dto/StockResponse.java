package com.example.demo.dto;

public record StockResponse(
        String symbol,
        String name,
        String currency,
        double price,
        double previousClose,
        double changePercent,
        double dayHigh,
        double dayLow,
        long volume
) {
}
