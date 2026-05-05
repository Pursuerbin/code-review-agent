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
public class LogicAgent implements CodeReviewAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KnowledgeVectorService knowledgeVectorService;

    public LogicAgent(KnowledgeVectorService knowledgeVectorService) {
        this.knowledgeVectorService = knowledgeVectorService;
    }

    @Override
    public String getAgentName() {
        return "LogicAgent";
    }

    @Override
    public String getAgentDescription() {
        return "逻辑审查专家 - 检查逻辑错误、边界条件、空指针、资源泄漏";
    }

    @Override
    public String review(String codeContent, String language) {
        List<String> relevantRules = knowledgeVectorService.searchSimilar(
                codeContent + " 空指针 边界条件 异常处理", 3);
        
        String knowledgeContext = "";
        if (!relevantRules.isEmpty()) {
            knowledgeContext = "\n\n【相关规范参考】\n" + String.join("\n", relevantRules);
        }

        String systemPrompt = "你是一位资深的逻辑分析专家，擅长发现代码中的逻辑错误和潜在问题。\n\n" +
                "请专注于以下方面：\n" +
                "1. 空指针检查：所有可能为 null 的对象在使用前是否进行了非空判断\n" +
                "2. 边界条件：数组越界、循环边界、参数范围校验\n" +
                "3. 逻辑错误：条件判断错误、分支覆盖不全、状态机错误\n" +
                "4. 资源泄漏：数据库连接、文件流、网络连接是否正确关闭\n" +
                "5. 异常处理：异常捕获是否完整，是否有适当的错误恢复机制\n" +
                "\n" +
                "请以结构化的方式输出，包含问题描述和改进建议。";

        String userPrompt = String.format("语言：%s\n\n代码：\n%s%s", language, codeContent, knowledgeContext);

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
            return "LogicAgent 调用失败: " + e.getMessage();
        }
        
        return "LogicAgent 无响应";
    }
}
