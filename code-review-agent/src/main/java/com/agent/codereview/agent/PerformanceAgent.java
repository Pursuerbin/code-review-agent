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
public class PerformanceAgent implements CodeReviewAgent {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KnowledgeVectorService knowledgeVectorService;

    public PerformanceAgent(KnowledgeVectorService knowledgeVectorService) {
        this.knowledgeVectorService = knowledgeVectorService;
    }

    @Override
    public String getAgentName() {
        return "PerformanceAgent";
    }

    @Override
    public String getAgentDescription() {
        return "性能审查专家 - 检查时间复杂度、数据库N+1查询、缓存策略";
    }

    @Override
    public String review(String codeContent, String language) {
        List<String> relevantRules = knowledgeVectorService.searchSimilar(
                codeContent + " 性能 缓存 N+1 查询 时间复杂度", 3);
        
        String knowledgeContext = "";
        if (!relevantRules.isEmpty()) {
            knowledgeContext = "\n\n【相关性能规范】\n" + String.join("\n", relevantRules);
        }

        String systemPrompt = "你是一位资深的性能优化专家，擅长发现代码中的性能瓶颈和优化机会。\n\n" +
                "请专注于以下性能方面：\n" +
                "1. 时间复杂度：算法复杂度是否最优，是否存在 O(n²) 可优化为 O(n log n)\n" +
                "2. 数据库查询：是否存在 N+1 查询问题，是否使用了合适的索引\n" +
                "3. 缓存策略：是否有重复计算，是否需要引入缓存\n" +
                "4. 内存使用：是否存在大对象频繁创建，是否有内存泄漏风险\n" +
                "5. 循环优化：循环内部是否有可移到外部的操作\n" +
                "6. 并发处理：是否可以并行处理，是否存在不必要的同步\n" +
                "\n" +
                "请以结构化的方式输出，包含性能影响评估和优化建议。";

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
            return "PerformanceAgent 调用失败: " + e.getMessage();
        }
        
        return "PerformanceAgent 无响应";
    }
}
