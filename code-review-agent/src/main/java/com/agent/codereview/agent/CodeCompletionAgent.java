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
public class CodeCompletionAgent implements UnifiedAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAgentName() {
        return "CodeCompletionAgent";
    }

    @Override
    public String getAgentDescription() {
        return "代码补全专家 - 补全不完整的代码片段";
    }

    @Override
    public String execute(String incompleteCode, String language, String additionalContext) {
        String systemPrompt = "你是一个代码补全工具。代码中可能包含 //TODO、//...、/* ... */ 等注释表示待补全部分，请根据上下文补全缺失代码。\n\n" +
                "要求：\n" +
                "1. 补全后的代码必须完整可运行\n" +
                "2. 保持原有代码风格和命名规范\n" +
                "3. 输出格式为 Markdown 代码块";

        String userPrompt = "语言：" + language + "\n不完整代码：\n" + incompleteCode;

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
            return "CodeCompletionAgent 调用失败: " + e.getMessage();
        }

        return "CodeCompletionAgent 无响应";
    }
}