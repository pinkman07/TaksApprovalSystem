package com.example.taskapprovalsystem.dto;

import com.example.taskapprovalsystem.entity.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private UserSummaryDTO creator;
    private List<ApproverStatus> approverStatuses;
    private List<CommentResponse> comments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverStatus {
        private Long approverId;
        private String name;
        private String email;
        private Boolean hasApproved;
        private LocalDateTime approvalDate;
    }

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
        this.creator = new UserSummaryDTO(task.getCreator());

        this.approverStatuses = new ArrayList<>();
        Map<Long, Approval> approvalMap = task.getApprovals().stream()
                .collect(Collectors.toMap(a -> a.getApprover().getId(), a -> a));

        for (User approver : task.getApprovers()) {
            Approval approval = approvalMap.get(approver.getId());
            approverStatuses.add(new ApproverStatus(
                    approver.getId(),
                    approver.getName(),
                    approver.getEmail(),
                    approval != null,
                    approval != null ? approval.getApprovalDate() : null
            ));
        }

        this.comments = task.getComments().stream()
                .map(CommentResponse::new)
                .collect(Collectors.toList());
    }
}