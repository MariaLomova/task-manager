package com.example.taskmanager.repo;

import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(Status status);
    Page<Task> findAllByStatus(Status status, Pageable pageable);
}