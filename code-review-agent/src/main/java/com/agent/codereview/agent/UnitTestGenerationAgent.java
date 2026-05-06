package com.agent.codereview.agent;

import com.agent.codereview.service.KnowledgeVectorService;
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
public class UnitTestGenerationAgent implements UnifiedAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KnowledgeVectorService knowledgeVectorService;

    public UnitTestGenerationAgent(KnowledgeVectorService knowledgeVectorService) {
        this.knowledgeVectorService = knowledgeVectorService;
    }

    @Override
    public String getAgentName() {
        return "UnitTestGenerationAgent";
    }

    @Override
    public String getAgentDescription() {
        return "单元测试专家 - 为源代码生成完整单元测试";
    }

    @Override
    public String execute(String sourceCode, String language, String additionalContext) {
        List<String> testExamples = knowledgeVectorService.searchSimilar(language + " 单元测试示例 JUnit Mockito", 2);

        String systemPrompt = "你是一位测试专家，请为以下代码生成完整的单元测试。\n\n" +
                "要求：\n" +
                "1. 覆盖正常场景、边界值、异常流程\n" +
                "2. 使用常见的" + language + "测试框架（JUnit、Mockito等）\n" +
                "3. Mock外部依赖\n" +
                "4. 输出格式为 Markdown 代码块";

        String userPrompt = "源代码：\n" + sourceCode;
        if (!testExamples.isEmpty()) {
            userPrompt += "\n\n参考测试风格：\n" + String.join("\n", testExamples);
        }

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
            return "UnitTestGenerationAgent 调用失败: " + e.getMessage();
        }

        return "UnitTestGenerationAgent 无响应";
    }
}