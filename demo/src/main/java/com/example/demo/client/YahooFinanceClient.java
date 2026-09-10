package com.example.demo.client;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class YahooFinanceClient {

    private final RestClient restClient;

    public YahooFinanceClient(RestClient.Builder builder) {
        this.restClient = builder
                // User-Agent가 없으면 Yahoo가 브라우저가 아닌 요청으로 판단해 429(Too Many Requests)를 반환함
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; SpringBootStudyBot/1.0)")
                .build();
    }

    public YahooChartResponse getChart(String symbol) {
        URI uri = UriComponentsBuilder.fromUriString("https://query1.finance.yahoo.com/v8/finance/chart/{symbol}")
                .buildAndExpand(symbol)
                .toUri();

        return restClient.get().uri(uri).retrieve().body(YahooChartResponse.class);
    }
}
