package com.agent.codereview.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long taskId;
    private Integer status;
    private String message;
    private LocalDateTime createdAt;
}