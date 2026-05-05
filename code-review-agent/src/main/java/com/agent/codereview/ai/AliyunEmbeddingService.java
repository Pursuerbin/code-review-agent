package com.agent.codereview.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AliyunEmbeddingService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将文本转换为向量（1536维）
     */
    public List<Double> embed(String text) {
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "text-embedding-v2");
        requestBody.put("input", text);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingArray = root.path("data").get(0).path("embedding");
                List<Double> vector = new ArrayList<>();
                for (JsonNode v : embeddingArray) {
                    vector.add(v.asDouble());
                }
                return vector;
            } catch (Exception e) {
                System.err.println("原始响应: " + response.getBody());
                throw new RuntimeException("解析 Embedding 响应失败", e);
            }
        } else {
            System.err.println("API 响应: " + response.getBody());
            throw new RuntimeException("Embedding API 调用失败: " + response.getStatusCode());
        }
    }
}
