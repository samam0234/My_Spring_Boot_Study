package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.WatchlistRequest;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// watchlist 추가 시 실제 Yahoo Finance API로 종목 존재 여부를 검증하므로 네트워크가 필요합니다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WatchlistControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("watchlist-test-user", passwordEncoder.encode("1234")));
        token = jwtTokenProvider.createToken("watchlist-test-user");
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addListAndRemoveFlow() throws Exception {
        String addJson = objectMapper.writeValueAsString(new WatchlistRequest("aapl"));

        mockMvc.perform(post("/watchlist")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/watchlist").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        mockMvc.perform(delete("/watchlist/AAPL").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/watchlist").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void addDuplicateFails() throws Exception {
        String addJson = objectMapper.writeValueAsString(new WatchlistRequest("MSFT"));

        mockMvc.perform(post("/watchlist")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/watchlist")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addJson))
                .andExpect(status().isConflict());
    }

    @Test
    void addInvalidSymbolFails() throws Exception {
        String addJson = objectMapper.writeValueAsString(new WatchlistRequest("ZZZZNOTASTOCK"));

        mockMvc.perform(post("/watchlist")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addJson))
                .andExpect(status().isNotFound());
    }
}
