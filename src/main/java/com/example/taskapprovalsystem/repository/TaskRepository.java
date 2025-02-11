package com.example.taskapprovalsystem.repository;

import com.example.taskapprovalsystem.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT DISTINCT t FROM Task t " +
            "LEFT JOIN FETCH t.approvers " +
            "LEFT JOIN FETCH t.creator")
    List<Task> findAllWithApprovers();
}
