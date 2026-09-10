package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.StorageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StorageControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("storage-test-user", passwordEncoder.encode("1234")));
        token = jwtTokenProvider.createToken("storage-test-user");
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/storage"))
                .andExpect(status().isForbidden());
    }

    @Test
    void putCreatesThenUpdatesSameKey() throws Exception {
        String first = objectMapper.writeValueAsString(new StorageRequest("첫 번째 값"));
        mockMvc.perform(put("/storage/memo").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("첫 번째 값"));

        String second = objectMapper.writeValueAsString(new StorageRequest("수정된 값"));
        mockMvc.perform(put("/storage/memo").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("수정된 값"));

        mockMvc.perform(get("/storage").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].value").value("수정된 값"));
    }

    @Test
    void getNonExistentKeyFails() throws Exception {
        mockMvc.perform(get("/storage/no-such-key").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesItem() throws Exception {
        String body = objectMapper.writeValueAsString(new StorageRequest("삭제될 값"));
        mockMvc.perform(put("/storage/temp").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/storage/temp").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/storage/temp").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
