package com.agent.codereview.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class CodeOptimizationAgent implements UnifiedAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAgentName() {
        return "CodeOptimizationAgent";
    }

    @Override
    public String getAgentDescription() {
        return "代码优化专家 - 从多维度优化代码质量";
    }

    @Override
    public String execute(String codeContent, String language, String additionalContext) {
        String systemPrompt = "你是一位代码优化专家。请从以下五个维度优化代码：\n\n" +
                "1. 可读性：命名规范、代码结构、注释质量\n" +
                "2. 性能：算法效率、内存使用、资源管理\n" +
                "3. 健壮性：异常处理、边界条件、空值检查\n" +
                "4. 最佳实践：设计模式、代码风格、API使用\n" +
                "5. 安全性：潜在安全风险、敏感信息处理\n\n" +
                "输出格式：\n" +
                "1. 先列出优化建议（分点说明）\n" +
                "2. 最后给出完整的优化后代码（Markdown 代码块）";

        String userPrompt = "语言：" + language + "\n\n待优化代码：\n" + codeContent;

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
            return "CodeOptimizationAgent 调用失败: " + e.getMessage();
        }

        return "CodeOptimizationAgent 无响应";
    }
}