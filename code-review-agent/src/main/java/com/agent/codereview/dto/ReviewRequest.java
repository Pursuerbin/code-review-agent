package com.agent.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotBlank(message = "代码内容不能为空")
    private String codeContent;
    private String lang;
}