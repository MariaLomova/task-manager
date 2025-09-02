package com.example.taskmanager.web;

import com.example.taskmanager.repo.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TaskRepository repo;

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    void create_and_get_task() throws Exception {
        String body = """
          {"title":"Test task","description":"via test","status":"NEW"}
        """;
        String createResp = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test task"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(createResp);
        long id = created.get("id").asLong();
        assertThat(id).isPositive();

        mvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Test task"));
    }

    @Test
    void list_and_filter_by_status() throws Exception {
        mvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"A\",\"status\":\"NEW\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"B\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/tasks").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void update_and_delete() throws Exception {
        String createResp = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"To update\",\"status\":\"NEW\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResp).get("id").asLong();

        mvc.perform(put("/api/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.status").value("DONE"));

        mvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isNotFound());
    }
}