package com.agent.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssistantRequest {
    
    @NotBlank
    private String actionType;
    
    @NotBlank
    private String input;
    
    private String language = "java";
    
    private String extraContext;
}