package com.example.demo.service;

import com.example.demo.client.YahooChartResponse;
import com.example.demo.client.YahooFinanceClient;
import com.example.demo.dto.StockResponse;
import com.example.demo.exception.StockNotFoundException;
import com.example.demo.exception.StockServiceUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Service
public class StockService {

    private final YahooFinanceClient yahooFinanceClient;

    public StockService(YahooFinanceClient yahooFinanceClient) {
        this.yahooFinanceClient = yahooFinanceClient;
    }

    public StockResponse getQuote(String symbol) {
        try {
            YahooChartResponse response = yahooFinanceClient.getChart(symbol);
            YahooChartResponse.Meta meta = response.chart().result().get(0).meta();

            return new StockResponse(
                    meta.symbol(),
                    meta.longName() != null ? meta.longName() : meta.shortName(),
                    meta.currency(),
                    meta.regularMarketPrice(),
                    meta.previousClose(),
                    meta.regularMarketChangePercent(),
                    meta.regularMarketDayHigh(),
                    meta.regularMarketDayLow(),
                    meta.regularMarketVolume()
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new StockNotFoundException(symbol);
        } catch (RestClientException e) {
            throw new StockServiceUnavailableException();
        }
    }
}
