package com.example.taskapprovalsystem.controller;

import com.example.taskapprovalsystem.dto.CommentDTO;
import com.example.taskapprovalsystem.dto.TaskDTO;
import com.example.taskapprovalsystem.entity.Comment;
import com.example.taskapprovalsystem.entity.Task;
import com.example.taskapprovalsystem.entity.TaskStatus;
import com.example.taskapprovalsystem.entity.User;
import com.example.taskapprovalsystem.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private ObjectMapper objectMapper;
    private Task mockTask;
    private User mockUser;
    private Comment mockComment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();

        // Setup mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");

        // Setup mock task
        mockTask = new Task();
        mockTask.setId(1L);
        mockTask.setTitle("Test Task");
        mockTask.setDescription("Test Description");
        mockTask.setStatus(TaskStatus.PENDING);
        mockTask.setCreator(mockUser);
        mockTask.setApprovers(new HashSet<>(Arrays.asList(mockUser)));

        // Setup mock comment
        mockComment = new Comment();
        mockComment.setId(1L);
        mockComment.setContent("Test Comment");
        mockComment.setUser(mockUser);
        mockComment.setTask(mockTask);
        mockComment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createTask_Success() throws Exception {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTitle("Test Task");
        taskDTO.setDescription("Test Description");
        taskDTO.setApproverIds(Arrays.asList(2L, 3L));

        when(taskService.createTask(any(TaskDTO.class), eq(1L))).thenReturn(mockTask);

        mockMvc.perform(post("/api/tasks")
                        .param("creatorId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    void approveTask_Success() throws Exception {
        when(taskService.approveTask(1L, 1L)).thenReturn(mockTask);

        mockMvc.perform(post("/api/tasks/1/approve")
                        .param("approverId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    void addComment_Success() throws Exception {
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setContent("Test Comment");

        when(taskService.addComment(eq(1L), eq(1L), any(CommentDTO.class))).thenReturn(mockComment);

        mockMvc.perform(post("/api/tasks/1/comments")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test Comment"));
    }

    @Test
    void getTask_Success() throws Exception {
        when(taskService.getTask(1L)).thenReturn(mockTask);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    void getAllTasks_Success() throws Exception {
        List<Task> tasks = Arrays.asList(mockTask);
        when(taskService.getAllTasks()).thenReturn(tasks);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

}