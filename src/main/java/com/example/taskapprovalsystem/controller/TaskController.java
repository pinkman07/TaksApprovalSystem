package com.example.taskapprovalsystem.controller;

import com.example.taskapprovalsystem.dto.*;
import com.example.taskapprovalsystem.entity.Comment;
import com.example.taskapprovalsystem.entity.Task;
import com.example.taskapprovalsystem.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task Management")
@Transactional
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    @ApiResponse(responseCode = "200", description = "Task created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid task request")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<TaskResponse> createTask(
            @RequestBody TaskDTO taskDTO,
            @RequestParam Long creatorId) {
        Task task = taskService.createTask(taskDTO, creatorId);
        return ResponseEntity.ok(new TaskResponse(task));
    }

    @PostMapping("/{taskId}/approve")
    @Operation(summary = "Approve task")
    @ApiResponse(responseCode = "200", description = "Task approved successfully")
    public ResponseEntity<TaskResponse> approveTask(
            @PathVariable Long taskId,
            @RequestParam Long approverId) {
        Task task = taskService.approveTask(taskId, approverId);
        return ResponseEntity.ok(new TaskResponse(task));
    }

    @PostMapping("/{taskId}/comments")
    @Operation(summary = "Add a comment to the task")
    @ApiResponse(responseCode = "200", description = "Comment added successfully")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long taskId,
            @RequestParam Long userId,
            @RequestBody CommentDTO commentDTO) {
        Comment comment = taskService.addComment(taskId, userId, commentDTO);
        return ResponseEntity.ok(new CommentResponse(comment));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID")
    @ApiResponse(responseCode = "200", description = "Task retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long taskId) {
        Task task = taskService.getTask(taskId);
        return ResponseEntity.ok(new TaskResponse(task));
    }

    @GetMapping
    @Operation(summary = "Get all tasks")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{taskId}")
    @Operation(summary = "Update task details")
    @ApiResponse(responseCode = "200", description = "Task updated successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskUpdateDTO updateDTO) {
        Task updatedTask = taskService.updateTask(taskId, updateDTO);
        return ResponseEntity.ok(new TaskResponse(updatedTask));
    }

}