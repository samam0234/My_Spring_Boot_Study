package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.MessageRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessageControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("msg-user-a", passwordEncoder.encode("1234")));
        userRepository.save(new User("msg-user-b", passwordEncoder.encode("1234")));
        tokenA = jwtTokenProvider.createToken("msg-user-a");
        tokenB = jwtTokenProvider.createToken("msg-user-b");
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/messages/inbox"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendMessageAppearsInReceiverInboxAndSenderSent() throws Exception {
        String json = objectMapper.writeValueAsString(new MessageRequest("msg-user-b", "안녕하세요"));

        mockMvc.perform(post("/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderUsername").value("msg-user-a"))
                .andExpect(jsonPath("$.receiverUsername").value("msg-user-b"));

        mockMvc.perform(get("/messages/inbox").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("안녕하세요"));

        mockMvc.perform(get("/messages/sent").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("안녕하세요"));
    }

    @Test
    void conversationReturnsMessagesInBothDirectionsChronologically() throws Exception {
        String toB = objectMapper.writeValueAsString(new MessageRequest("msg-user-b", "첫 메시지"));
        String toA = objectMapper.writeValueAsString(new MessageRequest("msg-user-a", "답장"));

        mockMvc.perform(post("/messages").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(toB))
                .andExpect(status().isOk());
        mockMvc.perform(post("/messages").header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(toA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/messages/conversation/msg-user-b").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("첫 메시지"))
                .andExpect(jsonPath("$[1].content").value("답장"));
    }

    @Test
    void sendToNonExistentUserFails() throws Exception {
        String json = objectMapper.writeValueAsString(new MessageRequest("no-such-user", "테스트"));

        mockMvc.perform(post("/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다: no-such-user"));
    }
}
