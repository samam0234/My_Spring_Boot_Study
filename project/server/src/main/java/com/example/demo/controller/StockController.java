package com.example.demo.controller;

import com.example.demo.dto.StockResponse;
import com.example.demo.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{symbol}")
    public StockResponse getQuote(@PathVariable String symbol) {
        return stockService.getQuote(symbol);
    }
}
