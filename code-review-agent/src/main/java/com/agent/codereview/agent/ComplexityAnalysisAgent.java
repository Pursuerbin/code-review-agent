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
public class ComplexityAnalysisAgent implements UnifiedAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAgentName() {
        return "ComplexityAnalysisAgent";
    }

    @Override
    public String getAgentDescription() {
        return "复杂度分析专家 - 分析代码复杂度指标并给出重构建议";
    }

    @Override
    public String execute(String codeContent, String language, String additionalContext) {
        String systemPrompt = "你是一位软件工程专家，请分析代码的复杂度。\n\n" +
                "需要分析：\n" +
                "1. 圈复杂度（Cyclomatic Complexity）\n" +
                "2. 认知复杂度（Cognitive Complexity）\n" +
                "3. 代码行数（LOC）\n" +
                "4. 嵌套深度（Nested Depth）\n" +
                "5. 方法数量和平均方法长度\n\n" +
                "输出格式：\n" +
                "1. 首先用 Markdown 表格输出复杂度指标\n" +
                "2. 然后解释其对维护性的影响\n" +
                "3. 最后给出具体的重构建议";

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
            return "ComplexityAnalysisAgent 调用失败: " + e.getMessage();
        }

        return "ComplexityAnalysisAgent 无响应";
    }
}