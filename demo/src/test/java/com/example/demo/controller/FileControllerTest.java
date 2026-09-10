package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("file-test-user", passwordEncoder.encode("1234")));
        token = jwtTokenProvider.createToken("file-test-user");
    }

    @Test
    void uploadWithoutTokenIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/files").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadAndDownloadFlow() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello world".getBytes());

        String response = mockMvc.perform(multipart("/files").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalName").value("test.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andReturn().getResponse().getContentAsString();

        String url = objectMapper.readTree(response).get("url").asString();

        // 다운로드는 인증 없이도 가능해야 함(공개 접근)
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string("hello world"));
    }

    @Test
    void downloadNonExistentFileFails() throws Exception {
        mockMvc.perform(get("/files/no-such-file.txt"))
                .andExpect(status().isNotFound());
    }
}
