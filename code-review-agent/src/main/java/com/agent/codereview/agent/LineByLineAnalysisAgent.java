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
public class LineByLineAnalysisAgent implements UnifiedAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAgentName() {
        return "LineByLineAnalysisAgent";
    }

    @Override
    public String getAgentDescription() {
        return "逐行分析专家 - 逐行检查代码潜在问题";
    }

    @Override
    public String execute(String codeContent, String language, String additionalContext) {
        String systemPrompt = "你是一位代码静态分析专家。请逐行分析以下代码，指出每一行可能存在的隐患、不规范写法或改进点。\n\n" +
                "输出格式要求：\n" +
                "使用 Markdown 表格格式输出：\n" +
                "| 行号 | 问题描述 | 严重程度 | 改进建议 |\n" +
                "|------|----------|----------|----------|\n";

        String userPrompt = "语言：" + language + "\n代码：\n" + codeContent;

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
            return "LineByLineAnalysisAgent 调用失败: " + e.getMessage();
        }

        return "LineByLineAnalysisAgent 无响应";
    }
}