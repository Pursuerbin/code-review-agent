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
public class SecurityAgent implements CodeReviewAgent, UnifiedAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KnowledgeVectorService knowledgeVectorService;

    public SecurityAgent(KnowledgeVectorService knowledgeVectorService) {
        this.knowledgeVectorService = knowledgeVectorService;
    }

    @Override
    public String getAgentName() {
        return "SecurityAgent";
    }

    @Override
    public String getAgentDescription() {
        return "安全审查专家 - 检查SQL注入、XSS、硬编码密码、权限越权";
    }

    @Override
    public String review(String codeContent, String language) {
        List<String> relevantRules = knowledgeVectorService.searchSimilar(
                codeContent + " SQL注入 XSS 安全 加密", 3);
        
        String knowledgeContext = "";
        if (!relevantRules.isEmpty()) {
            knowledgeContext = "\n\n【相关安全规范】\n" + String.join("\n", relevantRules);
        }

        String systemPrompt = "你是一位资深的安全专家，擅长发现代码中的安全漏洞和风险。\n\n" +
                "请专注于以下安全方面：\n" +
                "1. SQL 注入：是否使用参数化查询，禁止字符串拼接 SQL\n" +
                "2. XSS 攻击：用户输入是否进行了适当的转义或过滤\n" +
                "3. 敏感信息泄露：密码、密钥、Token 是否明文存储或打印\n" +
                "4. 硬编码敏感数据：密码、API Key、配置信息是否硬编码\n" +
                "5. 权限控制：是否存在越权访问风险\n" +
                "6. 输入验证：用户输入是否进行了严格验证\n" +
                "7. 加密安全：敏感数据是否使用安全的加密算法\n" +
                "\n" +
                "请以结构化的方式输出，包含风险等级（高/中/低）和改进建议。";

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
            return "SecurityAgent 调用失败: " + e.getMessage();
        }
        
        return "SecurityAgent 无响应";
    }

    @Override
    public String execute(String codeContent, String language, String additionalContext) {
        return review(codeContent, language);
    }
}
