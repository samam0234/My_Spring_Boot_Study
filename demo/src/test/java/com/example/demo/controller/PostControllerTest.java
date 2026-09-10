package com.example.demo.controller;

import com.example.demo.dto.PostRequest;
import com.example.demo.dto.PostUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requestsWithoutTokenAreAllowed() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void createFindUpdateDeleteFlow() throws Exception {
        String createJson = objectMapper.writeValueAsString(
                new PostRequest("첫 글", "안녕하세요, 첫 게시글입니다.", "철수"));

        String response = mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("첫 글"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/posts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("철수"));

        String updateJson = objectMapper.writeValueAsString(
                new PostUpdateRequest("수정된 제목", "수정된 내용입니다."));

        mockMvc.perform(put("/posts/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.author").value("철수"));

        mockMvc.perform(delete("/posts/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/posts/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다. id=" + id));
    }

    @Test
    void createWithBlankTitleFails() throws Exception {
        String invalidJson = objectMapper.writeValueAsString(new PostRequest("", "내용", "작성자"));

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listIsOrderedByLatestFirst() throws Exception {
        String older = objectMapper.writeValueAsString(new PostRequest("오래된 글", "내용1", "작성자1"));
        String newer = objectMapper.writeValueAsString(new PostRequest("최신 글", "내용2", "작성자2"));

        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content(older))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content(newer))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("최신 글"))
                .andExpect(jsonPath("$[1].title").value("오래된 글"));
    }
}
