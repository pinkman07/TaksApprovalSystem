package com.example.taskapprovalsystem.service;

import com.example.taskapprovalsystem.dto.CommentDTO;
import com.example.taskapprovalsystem.dto.TaskDTO;
import com.example.taskapprovalsystem.dto.TaskUpdateDTO;
import com.example.taskapprovalsystem.entity.*;
import com.example.taskapprovalsystem.exception.ResourceNotFoundException;
import com.example.taskapprovalsystem.exception.UnauthorizedOperationException;
import com.example.taskapprovalsystem.repository.CommentRepository;
import com.example.taskapprovalsystem.repository.TaskRepository;
import com.example.taskapprovalsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class TaskService {
    private static final String ADMIN_EMAIL = "test@@gmail.com";
    private static final String MANAGER_EMAIL = "test@gmail.com";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CommentRepository commentRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository,
                       EmailService emailService, CommentRepository commentRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.commentRepository = commentRepository;
    }

    public Task createTask(TaskDTO taskDTO, Long creatorId) {
        log.info("Creating new task with title: {} by creator ID: {}", taskDTO.getTitle(), creatorId);

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> {
                    log.error("Creator not found with ID: {}", creatorId);
                    return new ResourceNotFoundException("Creator not found");
                });

        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setStatus(TaskStatus.PENDING);
        task.setCreator(creator);

        Set<User> approvers = new HashSet<>();
        for (Long approverId : taskDTO.getApproverIds()) {
            User approver = userRepository.findById(approverId)
                    .orElseThrow(() -> {
                        log.error("Approver not found with ID: {}", approverId);
                        return new ResourceNotFoundException("Approver not found");
                    });
            approvers.add(approver);
        }
        task.setApprovers(approvers);

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {}", savedTask.getId());

        try {
            emailService.sendEmail(ADMIN_EMAIL, "New Task Created",
                    String.format("Task '%s' created by %s", task.getTitle(), creator.getName()));

            for (User approver : approvers) {
                emailService.sendEmail(approver.getEmail(), "New Task Requires Your Approval",
                        String.format("Task '%s' requires your approval", task.getTitle()));
            }
        } catch (Exception e) {
            log.error("Error sending notification emails for task: {}", savedTask.getId(), e);
        }

        return savedTask;
    }

    public Task approveTask(Long taskId, Long approverId) {
        log.info("Processing approval for task ID: {} by approver ID: {}", taskId, approverId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Task not found with ID: {}", taskId);
                    return new ResourceNotFoundException("Task not found");
                });

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> {
                    log.error("Approver not found with ID: {}", approverId);
                    return new ResourceNotFoundException("Approver not found");
                });

        if (!task.getApprovers().contains(approver)) {
            log.error("User {} is not an approver for task {}", approverId, taskId);
            throw new UnauthorizedOperationException("User is not an approver for this task");
        }

        Approval approval = new Approval();
        approval.setTask(task);
        approval.setApprover(approver);
        approval.setApprovalDate(LocalDateTime.now());
        approval.setApproved(true);
        task.getApprovals().add(approval);

        if (task.getApprovals().size() >= 3) {
            log.info("Task {} has received all required approvals", taskId);
            task.setStatus(TaskStatus.APPROVED);

            try {
                emailService.sendEmail(MANAGER_EMAIL, "Task Fully Approved",
                        String.format("Task '%s' has received all approvals", task.getTitle()));

                Set<String> emails = new HashSet<>();
                emails.add(task.getCreator().getEmail());
                task.getApprovers().forEach(a -> emails.add(a.getEmail()));

                emails.forEach(email ->
                        emailService.sendEmail(email, "Task Approved",
                                String.format("Task '%s' has been approved by all approvers", task.getTitle()))
                );
            } catch (Exception e) {
                log.error("Error sending approval notification emails for task: {}", taskId, e);
            }
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task {} updated with new approval from user {}", taskId, approverId);
        return updatedTask;
    }

    public Comment addComment(Long taskId, Long userId, CommentDTO commentDTO) {
        log.info("Adding comment to task ID: {} by user ID: {}", taskId, userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Task not found with ID: {}", taskId);
                    return new ResourceNotFoundException("Task not found");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setContent(commentDTO.getContent());
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment added successfully to task {} with ID: {}", taskId, savedComment.getId());
        return savedComment;
    }

    public Task getTask(Long taskId) {
        log.info("Fetching task with ID: {}", taskId);
        return taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Task not found with ID: {}", taskId);
                    return new ResourceNotFoundException("Task not found");
                });
    }

    public List<Task> getAllTasks() {
        log.info("Fetching all tasks with approvers");
        List<Task> tasks = taskRepository.findAllWithApprovers();

        tasks.forEach(task -> {
            task.getApprovals().size();
            task.getComments().size();
        });

        log.info("Retrieved {} tasks", tasks.size());
        return tasks;
    }

    @Transactional
    public Task updateTask(Long taskId, TaskUpdateDTO updateDTO) {
        log.info("Updating task ID: {} with new details", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Task not found with ID: {}", taskId);
                    return new ResourceNotFoundException("Task not found");
                });

        if (updateDTO.getTitle() != null) {
            task.setTitle(updateDTO.getTitle());
        }
        if (updateDTO.getDescription() != null) {
            task.setDescription(updateDTO.getDescription());
        }

        if (updateDTO.getApproverIds() != null && !updateDTO.getApproverIds().isEmpty()) {
            Set<User> newApprovers = new HashSet<>();
            newApprovers.addAll(task.getApprovers());

            for (Long approverId : updateDTO.getApproverIds()) {
                User approver = userRepository.findById(approverId)
                        .orElseThrow(() -> {
                            log.error("Approver not found with ID: {}", approverId);
                            return new ResourceNotFoundException("Approver not found");
                        });
                newApprovers.add(approver);
            }

            task.setApprovers(newApprovers);

            updateDTO.getApproverIds().stream()
                    .filter(id -> !task.getApprovers().stream()
                            .map(User::getId)
                            .collect(Collectors.toSet())
                            .contains(id))
                    .forEach(newApproverId -> {
                        User newApprover = userRepository.findById(newApproverId)
                                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));
                        try {
                            emailService.sendEmail(
                                    newApprover.getEmail(),
                                    "New Task Requires Your Approval",
                                    String.format("Task '%s' requires your approval", task.getTitle())
                            );
                        } catch (Exception e) {
                            log.error("Failed to send email to new approver: {}", newApproverId, e);
                        }
                    });
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task {} updated successfully", taskId);
        return updatedTask;
    }
}