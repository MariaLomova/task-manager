package com.example.taskmanager.web;

import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.repo.TaskRepository;
import com.example.taskmanager.web.dto.TaskCreateRequest;
import com.example.taskmanager.web.dto.TaskResponse;
import com.example.taskmanager.web.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository repo;

    public TaskController(TaskRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreateRequest req) {
        Task task = new Task(
                req.getTitle(),
                req.getDescription(),
                req.getStatus() == null ? Status.NEW : req.getStatus()
        );
        Task saved = repo.save(task);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/api/tasks/" + saved.getId());
        return new ResponseEntity<>(toResp(saved), headers, HttpStatus.CREATED);
    }

    @GetMapping
    public Page<TaskResponse> list(
            @RequestParam(name = "status", required = false) Status status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        String[] parts = sort.split(",");
        Sort s = (parts.length == 2)
                ? Sort.by(Sort.Direction.fromString(parts[1]), parts[0])
                : Sort.by(Sort.Direction.DESC, "createdAt");

        PageRequest pr = PageRequest.of(page, size, s);
        Page<Task> result = (status == null)
                ? repo.findAll(pr)
                : repo.findAllByStatus(status, pr);

        return result.map(this::toResp);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable(name = "id") Long id) {
        Task t = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found")
        );
        return toResp(t);
    }

    @PutMapping("/{id}")
    @Transactional
    public TaskResponse update(@PathVariable(name = "id") Long id,
                               @RequestBody TaskUpdateRequest req) {
        Task t = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found")
        );
        if (req.getTitle() != null && !req.getTitle().isBlank()) t.setTitle(req.getTitle());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getStatus() != null) t.setStatus(req.getStatus());
        Task saved = repo.save(t);
        return toResp(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable(name = "id") Long id) {
        try {
            repo.deleteById(id);
        } catch (EmptyResultDataAccessException ignored) {
            // если задачи нет, то просто ничего не делаем
        }
    }

    private TaskResponse toResp(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }
}