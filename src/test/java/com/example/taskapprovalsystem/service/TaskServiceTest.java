package com.example.taskapprovalsystem.service;

import com.example.taskapprovalsystem.dto.CommentDTO;
import com.example.taskapprovalsystem.dto.TaskDTO;
import com.example.taskapprovalsystem.entity.*;
import com.example.taskapprovalsystem.exception.ResourceNotFoundException;
import com.example.taskapprovalsystem.exception.UnauthorizedOperationException;
import com.example.taskapprovalsystem.repository.CommentRepository;
import com.example.taskapprovalsystem.repository.TaskRepository;
import com.example.taskapprovalsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private TaskService taskService;

    private User creator;
    private User approver;
    private Task task;
    private TaskDTO taskDTO;
    private Comment comment;
    private CommentDTO commentDTO;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setName("Creator");
        creator.setEmail("creator@example.com");

        approver = new User();
        approver.setId(2L);
        approver.setName("Approver");
        approver.setEmail("approver@example.com");

        task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setStatus(TaskStatus.PENDING);
        task.setCreator(creator);
        task.setApprovers(new HashSet<>(Collections.singletonList(approver)));
        task.setApprovals(new ArrayList<>());
        task.setComments(new ArrayList<>());

        taskDTO = new TaskDTO();
        taskDTO.setTitle("Test Task");
        taskDTO.setDescription("Test Description");
        taskDTO.setApproverIds(Collections.singletonList(2L));

        comment = new Comment();
        comment.setId(1L);
        comment.setContent("Test Comment");
        comment.setUser(creator);
        comment.setTask(task);
        comment.setCreatedAt(LocalDateTime.now());

        commentDTO = new CommentDTO();
        commentDTO.setContent("Test Comment");
    }

    @Test
    void createTask_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        Task result = taskService.createTask(taskDTO, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(taskDTO.getTitle());
        assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_CreatorNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.createTask(taskDTO, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Creator not found");
    }

    @Test
    void approveTask_Success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.approveTask(1L, 2L);

        assertThat(result).isNotNull();
        assertThat(result.getApprovals()).hasSize(1);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void approveTask_UnauthorizedApprover() {
        User unauthorizedUser = new User();
        unauthorizedUser.setId(3L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(3L)).thenReturn(Optional.of(unauthorizedUser));

        assertThatThrownBy(() -> taskService.approveTask(1L, 3L))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessage("User is not an approver for this task");
    }

    @Test
    void approveTask_AllApprovalsReceived() {
        task.setApprovers(new HashSet<>(Arrays.asList(
                approver,
                createApprover(3L, "Approver2"),
                createApprover(4L, "Approver3")
        )));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        addApproval(task, 3L);
        addApproval(task, 4L);

        Task result = taskService.approveTask(1L, 2L);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.APPROVED);
        verify(emailService, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void addComment_Success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment result = taskService.addComment(1L, 1L, commentDTO);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(commentDTO.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void getAllTasks_Success() {
        List<Task> tasks = Collections.singletonList(task);
        when(taskRepository.findAllWithApprovers()).thenReturn(tasks);

        List<Task> result = taskService.getAllTasks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Task");
    }

    private User createApprover(Long id, String name) {
        User approver = new User();
        approver.setId(id);
        approver.setName(name);
        approver.setEmail(name.toLowerCase() + "@example.com");
        return approver;
    }

    private void addApproval(Task task, Long approverId) {
        Approval approval = new Approval();
        approval.setApprover(createApprover(approverId, "Approver" + approverId));
        approval.setApprovalDate(LocalDateTime.now());
        approval.setApproved(true);
        approval.setTask(task);
        task.getApprovals().add(approval);
    }
}