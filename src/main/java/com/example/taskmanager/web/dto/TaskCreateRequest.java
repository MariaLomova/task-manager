package com.example.taskmanager.web.dto;

import com.example.taskmanager.model.Status;
import jakarta.validation.constraints.NotBlank;

public class TaskCreateRequest {
    @NotBlank
    private String title;
    private String description;
    private Status status;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}