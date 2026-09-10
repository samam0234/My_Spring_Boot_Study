package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("notif-test-user", passwordEncoder.encode("1234")));
        token = jwtTokenProvider.createToken("notif-test-user");
    }

    @Test
    void streamWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/notifications/stream"))
                .andExpect(status().isForbidden());
    }

    @Test
    void streamWithTokenStartsAsyncConnection() throws Exception {
        mockMvc.perform(get("/notifications/stream").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted());
    }

    @Test
    void streamAcceptsTokenAsQueryParam() throws Exception {
        // 브라우저 EventSource는 커스텀 헤더를 못 붙이므로 쿼리 파라미터 인증도 지원해야 함
        mockMvc.perform(get("/notifications/stream").param("token", token))
                .andExpect(request().asyncStarted());
    }
}
