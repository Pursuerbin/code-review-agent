package com.agent.codereview.service.impl;

import com.agent.codereview.agent.MultiAgentReviewService;
import com.agent.codereview.agent.UnifiedAgent;
import com.agent.codereview.entity.ReviewTask;
import com.agent.codereview.mapper.ReviewTaskMapper;
import com.agent.codereview.service.ReviewTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskMapper reviewTaskMapper;
    private final MultiAgentReviewService multiAgentReviewService;
    private final Map<String, UnifiedAgent> agentMap = new HashMap<>();

    public ReviewTaskServiceImpl(ReviewTaskMapper reviewTaskMapper, 
                                 MultiAgentReviewService multiAgentReviewService,
                                 List<UnifiedAgent> agents) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.multiAgentReviewService = multiAgentReviewService;
        for (UnifiedAgent agent : agents) {
            agentMap.put(agent.getAgentName(), agent);
            log.info("注册 UnifiedAgent: {}", agent.getAgentName());
        }
    }

    @Override
    @Transactional
    public ReviewTask createTask(String codeContent, String lang) {
        ReviewTask task = new ReviewTask();
        task.setCodeContent(codeContent);
        task.setLang(lang != null ? lang : "java");
        task.setStatus(0);
        task.setTaskType("REVIEW");
        reviewTaskMapper.insert(task);
        
        doReview(task.getId());
        return task;
    }

    @Override
    @Transactional
    public ReviewTask createAssistantTask(String codeContent, String lang, String taskType, String extraContext) {
        ReviewTask task = new ReviewTask();
        task.setCodeContent(codeContent);
        task.setLang(lang != null ? lang : "java");
        task.setStatus(0);
        task.setTaskType(taskType);
        task.setExtraContext(extraContext);
        reviewTaskMapper.insert(task);
        
        doAssistantTask(task.getId());
        return task;
    }

    @Async
    public void doReview(Long taskId) {
        log.info("开始审查任务: {}", taskId);
        try {
            ReviewTask task = reviewTaskMapper.selectById(taskId);
            task.setStatus(1);
            reviewTaskMapper.updateById(task);

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

    @Async
    public void doAssistantTask(Long taskId) {
        log.info("开始智能助理任务: {}", taskId);
        try {
            ReviewTask task = reviewTaskMapper.selectById(taskId);
            task.setStatus(1);
            reviewTaskMapper.updateById(task);

            UnifiedAgent agent = agentMap.get(task.getTaskType());
            if (agent == null) {
                throw new RuntimeException("未知的任务类型: " + task.getTaskType());
            }

            String result = agent.execute(task.getCodeContent(), task.getLang(), task.getExtraContext());

            task.setStatus(2);
            task.setResultSummary(result);
            reviewTaskMapper.updateById(task);
            log.info("智能助理任务完成: {}", taskId);
        } catch (Exception e) {
            log.error("智能助理任务失败: {}", taskId, e);
            ReviewTask task = reviewTaskMapper.selectById(taskId);
            task.setStatus(3);
            task.setResultSummary("执行失败：" + e.getMessage());
            reviewTaskMapper.updateById(task);
        }
    }

    @Override
    public ReviewTask getTask(Long id) {
        return reviewTaskMapper.selectById(id);
    }
}