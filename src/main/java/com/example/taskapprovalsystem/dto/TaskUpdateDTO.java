package com.example.taskapprovalsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateDTO {
    private String title;
    private String description;
    private List<Long> approverIds;
}