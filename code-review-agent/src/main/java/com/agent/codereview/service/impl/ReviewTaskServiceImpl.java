package com.agent.codereview.service.impl;

import com.agent.codereview.agent.MultiAgentReviewService;
import com.agent.codereview.entity.ReviewTask;
import com.agent.codereview.mapper.ReviewTaskMapper;
import com.agent.codereview.service.ReviewTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskMapper reviewTaskMapper;
    
    // ========== 切换到多 Agent 服务 ==========
    // 使用 MultiAgentReviewService 实现并行审查
    private final MultiAgentReviewService multiAgentReviewService;

    @Override
    @Transactional
    public ReviewTask createTask(String codeContent, String lang) {
        ReviewTask task = new ReviewTask();
        task.setCodeContent(codeContent);
        task.setLang(lang != null ? lang : "java");
        task.setStatus(0);
        reviewTaskMapper.insert(task);
        
        doReview(task.getId());
        return task;
    }

    @Async
    public void doReview(Long taskId) {
        log.info("开始审查任务: {}", taskId);
        try {
            ReviewTask task = reviewTaskMapper.selectById(taskId);
            task.setStatus(1);
            reviewTaskMapper.updateById(task);

            // ========== 使用多 Agent 并行审查 ==========
            // 三个 Agent 并行执行，最后汇总结果
            String result = multiAgentReviewService.reviewWithMultiAgents(
                    task.getCodeContent(), 
                    task.getLang()
            );

            task.setStatus(2);
            task.setResultSummary(result);
            reviewTaskMapper.updateById(task);
            log.info("审查任务完成: {}", taskId);
        } catch (Exception e) {
            log.error("审查任务失败: {}", taskId, e);
            ReviewTask task = reviewTaskMapper.selectById(taskId);
            task.setStatus(3);
            task.setResultSummary("审查失败：" + e.getMessage());
            reviewTaskMapper.updateById(task);
        }
    }

    @Override
    public ReviewTask getTask(Long id) {
        return reviewTaskMapper.selectById(id);
    }
}