package com.example.taskmanager.web;

import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.repo.TaskRepository;
import com.example.taskmanager.web.dto.TaskCreateRequest;
import com.example.taskmanager.web.dto.TaskResponse;
import com.example.taskmanager.web.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository repo;

    public TaskController(TaskRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskCreateRequest req) {
        Task task = new Task(
                req.getTitle(),
                req.getDescription(),
                req.getStatus() == null ? Status.NEW : req.getStatus()
        );
        Task saved = repo.save(task);
        return toResp(saved);
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(name = "status", required = false) Status status) {
        List<Task> tasks = (status == null) ? repo.findAll() : repo.findByStatus(status);
        return tasks.stream().map(this::toResp).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        Task t = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found")
        );
        return toResp(t);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @RequestBody TaskUpdateRequest req) {
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
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        repo.deleteById(id);
    }

    private TaskResponse toResp(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getCreatedAt());
    }
}