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
public class CodeGenerationAgent implements UnifiedAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KnowledgeVectorService knowledgeVectorService;

    public CodeGenerationAgent(KnowledgeVectorService knowledgeVectorService) {
        this.knowledgeVectorService = knowledgeVectorService;
    }

    @Override
    public String getAgentName() {
        return "CodeGenerationAgent";
    }

    @Override
    public String getAgentDescription() {
        return "代码生成专家 - 根据需求描述生成高质量代码";
    }

    @Override
    public String execute(String description, String language, String additionalContext) {
        List<String> examples = knowledgeVectorService.searchSimilar(description + " " + language, 2);

        String systemPrompt = "你是一位资深的" + language + "开发专家，请根据需求描述生成高质量的代码。\n\n" +
                "要求：\n" +
                "1. 代码可运行，包含必要的注释\n" +
                "2. 遵循最佳实践和设计模式\n" +
                "3. 包含异常处理和边界条件检查\n" +
                "4. 输出格式为 Markdown 代码块";

        String userPrompt = "需求描述：" + description + "\n";
        if (!examples.isEmpty()) {
            userPrompt += "\n参考示例代码：\n" + String.join("\n", examples) + "\n";
        }
        userPrompt += "\n请生成" + language + "代码：";

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
            return "CodeGenerationAgent 调用失败: " + e.getMessage();
        }

        return "CodeGenerationAgent 无响应";
    }
}