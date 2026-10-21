package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 Open-Meteo API를 호출하는 통합 테스트라 네트워크가 필요합니다.
@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getWeatherForKnownCitySucceeds() throws Exception {
        mockMvc.perform(get("/weather").param("city", "Seoul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").exists())
                .andExpect(jsonPath("$.temperature").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    void getWeatherForUnknownCityFails() throws Exception {
        mockMvc.perform(get("/weather").param("city", "asdfqwerzzzznotacity"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingCityParamReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/weather"))
                .andExpect(status().isBadRequest());
    }
}
