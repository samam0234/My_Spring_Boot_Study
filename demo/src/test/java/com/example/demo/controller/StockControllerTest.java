package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 Yahoo Finance API를 호출하는 통합 테스트라 네트워크가 필요합니다.
@SpringBootTest
@AutoConfigureMockMvc
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getQuoteForKnownSymbolSucceeds() throws Exception {
        mockMvc.perform(get("/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").exists());
    }

    @Test
    void getQuoteForUnknownSymbolFails() throws Exception {
        mockMvc.perform(get("/stocks/ZZZZNOTASTOCK"))
                .andExpect(status().isNotFound());
    }
}
