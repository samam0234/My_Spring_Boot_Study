package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signupSucceeds() throws Exception {
        SignupRequest request = new SignupRequest("tester1", "1234");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void signupWithDuplicateUsernameFails() throws Exception {
        String json = objectMapper.writeValueAsString(new SignupRequest("tester2", "1234"));

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다: tester2"));
    }

    @Test
    void signupWithTooShortFieldsFails() throws Exception {
        SignupRequest request = new SignupRequest("ab", "12");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        String signupJson = objectMapper.writeValueAsString(new SignupRequest("tester3", "1234"));
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated());

        String loginJson = objectMapper.writeValueAsString(new LoginRequest("tester3", "wrong"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSucceedsAndReturnsToken() throws Exception {
        String signupJson = objectMapper.writeValueAsString(new SignupRequest("tester4", "1234"));
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated());

        String loginJson = objectMapper.writeValueAsString(new LoginRequest("tester4", "1234"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
