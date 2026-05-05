package com.agent.codereview.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MultiAgentReviewService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LogicAgent logicAgent;
    private final SecurityAgent securityAgent;
    private final PerformanceAgent performanceAgent;

    public MultiAgentReviewService(LogicAgent logicAgent, 
                                   SecurityAgent securityAgent, 
                                   PerformanceAgent performanceAgent) {
        this.logicAgent = logicAgent;
        this.securityAgent = securityAgent;
        this.performanceAgent = performanceAgent;
    }

    public String reviewWithMultiAgents(String codeContent, String language) {
        log.info("开始多 Agent 并行审查...");
        
        long startTime = System.currentTimeMillis();

        CompletableFuture<String> logicFuture = CompletableFuture.supplyAsync(() -> {
            log.info("LogicAgent 开始审查...");
            String result = logicAgent.review(codeContent, language);
            log.info("LogicAgent 审查完成");
            return result;
        });

        CompletableFuture<String> securityFuture = CompletableFuture.supplyAsync(() -> {
            log.info("SecurityAgent 开始审查...");
            String result = securityAgent.review(codeContent, language);
            log.info("SecurityAgent 审查完成");
            return result;
        });

        CompletableFuture<String> performanceFuture = CompletableFuture.supplyAsync(() -> {
            log.info("PerformanceAgent 开始审查...");
            String result = performanceAgent.review(codeContent, language);
            log.info("PerformanceAgent 审查完成");
            return result;
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                logicFuture, securityFuture, performanceFuture);

        try {
            allFutures.get();
            
            String logicResult = logicFuture.get();
            String securityResult = securityFuture.get();
            String performanceResult = performanceFuture.get();

            String summary = summarizeResults(logicResult, securityResult, performanceResult);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("多 Agent 审查完成，耗时: {}ms", totalTime);
            
            return summary;
            
        } catch (Exception e) {
            log.error("多 Agent 审查失败", e);
            return "审查失败：" + e.getMessage();
        }
    }

    private String summarizeResults(String logicResult, String securityResult, String performanceResult) {
        String systemPrompt = "你是一位资深的代码审查报告整理专家。\n\n" +
                "请将以下三份独立的审查意见整合成一份结构化的、专业的代码审查报告。\n\n" +
                "要求：\n" +
                "1. 结构清晰：分逻辑、安全、性能三个维度展示\n" +
                "2. 去重合并：如果多个 Agent 提出了相同或相似的问题，进行合并\n" +
                "3. 优先级排序：按照问题严重程度排序（严重 > 中等 > 建议）\n" +
                "4. 格式规范：使用 Markdown 格式，包含问题描述、影响分析、改进建议\n" +
                "5. 语言自然：以友好、专业的语言呈现给开发者\n" +
                "\n" +
                "输出格式：\n" +
                "# 代码审查报告\n\n" +
                "## 概览\n" +
                "（简要说明整体审查结果）\n\n" +
                "## 问题列表\n" +
                "### 严重问题\n" +
                "- [问题描述] - [影响分析] - [改进建议]\n\n" +
                "### 中等问题\n" +
                "- [问题描述] - [影响分析] - [改进建议]\n\n" +
                "### 优化建议\n" +
                "- [问题描述] - [改进建议]\n\n" +
                "## 总结\n" +
                "（总结审查结果和建议）";

        String userPrompt = String.format("请整合以下三份代码审查意见：\n\n" +
                "=== 逻辑审查意见 ===\n%s\n\n" +
                "=== 安全审查意见 ===\n%s\n\n" +
                "=== 性能审查意见 ===\n%s", 
                logicResult, securityResult, performanceResult);

        return callApi(systemPrompt, userPrompt);
    }

    private String callApi(String systemPrompt, String userPrompt) {
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> userMessage = Map.of(
            "role", "user",
            "content", userPrompt
        );

        Map<String, Object> systemMessage = Map.of(
            "role", "system",
            "content", systemPrompt
        );

        Map<String, Object> requestBody = Map.of(
            "model", "qwen-turbo",
            "messages", List.of(systemMessage, userMessage)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            var response = restTemplate.postForObject(url, entity, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                return root.path("choices").get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            return "汇总失败: " + e.getMessage();
        }
        
        return "无法生成汇总报告";
    }

    public List<AgentInfo> getAgentInfoList() {
        List<AgentInfo> agents = new ArrayList<>();
        agents.add(new AgentInfo(logicAgent.getAgentName(), logicAgent.getAgentDescription()));
        agents.add(new AgentInfo(securityAgent.getAgentName(), securityAgent.getAgentDescription()));
        agents.add(new AgentInfo(performanceAgent.getAgentName(), performanceAgent.getAgentDescription()));
        return agents;
    }

    public record AgentInfo(String name, String description) {}
}
