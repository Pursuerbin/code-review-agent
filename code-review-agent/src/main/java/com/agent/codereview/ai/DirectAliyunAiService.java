package com.agent.codereview.ai;

import com.agent.codereview.service.KnowledgeVectorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DirectAliyunAiService {

    private final ChatClient chatClient;
    private final KnowledgeVectorService knowledgeVectorService;

    @Autowired
    public DirectAliyunAiService(ChatClient.Builder chatClientBuilder, KnowledgeVectorService knowledgeVectorService) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一位资深的代码审查专家，精通 Java、Spring Boot、MySQL、Redis 等技术栈。"
                        + "请分析用户提供的代码片段，指出潜在的问题（空指针、性能、安全、可读性等），"
                        + "并给出具体的改进建议。如果提供了编码规范，请引用规范中的具体要求。"
                        + "输出格式使用 Markdown。")
                .build();
        this.knowledgeVectorService = knowledgeVectorService;
    }

    public String reviewCode(String codeContent, String language) {
        List<String> relevantRules = knowledgeVectorService.searchSimilar(codeContent, 3);
        String knowledgeContext = "";
        if (!relevantRules.isEmpty()) {
            knowledgeContext = "\n\n【相关编码规范】\n" + String.join("\n", relevantRules);
        }

        String userPrompt = String.format("语言：%s\n\n代码：\n%s%s", language, codeContent, knowledgeContext);

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }
}
