package com.agent.codereview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantResponse {
    
    private String status;
    
    private String taskId;
    
    public static AssistantResponse success(String taskId) {
        return new AssistantResponse("ok", taskId);
    }
    
    public static AssistantResponse error(String message) {
        return new AssistantResponse("error", message);
    }
}