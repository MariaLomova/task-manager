package com.example.taskmanager.web;

import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.repo.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    TaskRepository repo;

    private Task newTask(String title, String desc, Status st) {
        return new Task(title, desc, st);
    }

    private Task withId(Task t, Long id) {
        try {
            Field f = Task.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, id);
            return t;
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id via reflection", e);
        }
    }

    @Test
    @DisplayName("POST /api/tasks — 201 + Location + тело ответа")
    void create() throws Exception {
        // когда контроллер вызывает repo.save(новая задача) — возвращаем ту же, но уже с id
        ArgumentMatcher<Task> toSaveMatcher = t ->
                t.getId() == null &&
                "New".equals(t.getTitle()) &&
                "desc".equals(t.getDescription()) &&
                t.getStatus() == Status.NEW;

        Task saved = withId(newTask("New", "desc", Status.NEW), 1L);
        given(repo.save(argThat(toSaveMatcher))).willReturn(saved);

        String body = """
            {"title":"New","description":"desc","status":"NEW"}
            """;

        mvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/1"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("New")))
                .andExpect(jsonPath("$.description", is("desc")))
                .andExpect(jsonPath("$.status", is("NEW")));
    }

    @Test
    @DisplayName("GET /api/tasks — страница задач без фильтра (content[])")
    void listPaged() throws Exception {
        Task t1 = withId(newTask("A", "d", Status.NEW), 10L);

        Page<Task> page = new PageImpl<>(
                List.of(t1),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        given(repo.findAll(eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))))
                .willReturn(page);

        mvc.perform(get("/api/tasks")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(10)))
                .andExpect(jsonPath("$.content[0].title", is("A")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/tasks?status=IN_PROGRESS — фильтрация + пагинация")
    void listByStatusPaged() throws Exception {
        Task t2 = withId(newTask("B", "d", Status.IN_PROGRESS), 20L);

        Page<Task> page = new PageImpl<>(
                List.of(t2),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "createdAt")),
                1
        );

        given(repo.findAllByStatus(
                eq(Status.IN_PROGRESS),
                eq(PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "createdAt")))
        )).willReturn(page);

        mvc.perform(get("/api/tasks")
                .param("status", "IN_PROGRESS")
                .param("page", "0")
                .param("size", "5")
                .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(20)))
                .andExpect(jsonPath("$.content[0].status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} — вернуть одну задачу")
    void getById() throws Exception {
        Task t = withId(newTask("Read", "d", Status.DONE), 7L);
        given(repo.findById(7L)).willReturn(Optional.of(t));

        mvc.perform(get("/api/tasks/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.title", is("Read")))
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} — частичное обновление")
    void update() throws Exception {
        Task existed = withId(newTask("Old", "d", Status.NEW), 3L);
        Task updated = withId(newTask("NewTitle", "d2", Status.DONE), 3L);

        given(repo.findById(3L)).willReturn(Optional.of(existed));
        given(repo.save(existed)).willReturn(updated);

        String body = """
            {"title":"NewTitle","description":"d2","status":"DONE"}
            """;

        mvc.perform(put("/api/tasks/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.title", is("NewTitle")))
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} — 204 и идемпотентность")
    void deleteOk() throws Exception {
        doNothing().when(repo).deleteById(5L);

        mvc.perform(delete("/api/tasks/5"))
                .andExpect(status().isNoContent());
    }
}