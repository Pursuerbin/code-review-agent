package com.agent.codereview.controller;

import com.agent.codereview.dto.ReviewRequest;
import com.agent.codereview.dto.ReviewResponse;
import com.agent.codereview.entity.ReviewTask;
import com.agent.codereview.service.ReviewTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewTaskService reviewTaskService;

    @PostMapping("/submit")
    public ResponseEntity<ReviewResponse> submitTask(@Valid @RequestBody ReviewRequest request) {
        ReviewTask task = reviewTaskService.createTask(request.getCodeContent(), request.getLang());
        
        ReviewResponse response = new ReviewResponse();
        response.setTaskId(task.getId());
        response.setStatus(task.getStatus());
        response.setMessage("任务已创建，等待审查");
        response.setCreatedAt(task.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/task/{id}")
    public ResponseEntity<ReviewTask> getTask(@PathVariable Long id) {
        ReviewTask task = reviewTaskService.getTask(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }
}