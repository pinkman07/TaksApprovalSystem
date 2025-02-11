package com.example.taskapprovalsystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskDTO {
    private String title;
    private String description;
    private List<Long> approverIds;
}