package com.agent.codereview.service;

import com.agent.codereview.entity.ReviewTask;

public interface ReviewTaskService {
    ReviewTask createTask(String codeContent, String lang);
    ReviewTask getTask(Long id);
}