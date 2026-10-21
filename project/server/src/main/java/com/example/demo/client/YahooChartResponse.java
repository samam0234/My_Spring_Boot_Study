package com.example.demo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooChartResponse(Chart chart) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chart(List<Result> result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(Meta meta) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            String symbol,
            String currency,
            String exchangeName,
            String longName,
            String shortName,
            double regularMarketPrice,
            double previousClose,
            double regularMarketChangePercent,
            double regularMarketDayHigh,
            double regularMarketDayLow,
            long regularMarketVolume
    ) {
    }
}
