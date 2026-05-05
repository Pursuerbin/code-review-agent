package com.agent.codereview.ai;

import com.agent.codereview.service.KnowledgeVectorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 Spring AI ChatClient 调用阿里云 API 的代码审查服务
 * 阿里云百炼平台提供了 OpenAI 兼容的 API，因此可以直接使用 Spring AI 的 OpenAI 适配器
 */
@Service
public class SpringAiCodeReviewService {

    private final ChatClient chatClient;
    private final KnowledgeVectorService knowledgeVectorService;

    // 构造函数注入：Spring AI 会自动提供 ChatClient.Builder
    @Autowired
    public SpringAiCodeReviewService(ChatClient.Builder chatClientBuilder, 
                                     KnowledgeVectorService knowledgeVectorService) {
        // 配置 ChatClient，设置默认的系统提示词
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一位资深的代码审查专家，精通 Java、Spring Boot、MySQL、Redis 等技术栈。"
                        + "请分析用户提供的代码片段，指出潜在的问题（空指针、性能、安全、可读性等），"
                        + "并给出具体的改进建议。如果提供了编码规范，请引用规范中的具体要求。"
                        + "输出格式使用 Markdown。")
                .build();
        
        this.knowledgeVectorService = knowledgeVectorService;
    }

    /**
     * 审查代码（使用 Spring AI ChatClient）
     * @param codeContent 待审查的代码内容
     * @param language 编程语言（如 java, python, javascript 等）
     * @return 审查报告（Markdown 格式）
     */
    public String reviewCode(String codeContent, String language) {
        // ========== RAG 检索增强 ==========
        // 从知识库中检索与代码相关的编码规范
        List<String> relevantRules = knowledgeVectorService.searchSimilar(codeContent, 3);
        
        // 构建知识库上下文
        String knowledgeContext = "";
        if (!relevantRules.isEmpty()) {
            knowledgeContext = "\n\n【相关编码规范】\n" + String.join("\n", relevantRules);
        }

        // ========== 构造用户消息 ==========
        String userPrompt = String.format("语言：%s\n\n代码：\n%s%s", language, codeContent, knowledgeContext);

        // ========== 使用 Spring AI ChatClient 调用 AI ==========
        // 方式一：使用简化的链式调用（推荐）
        String result = chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        return result;
    }

    /**
     * 审查代码（手动构建消息列表，更灵活的方式）
     * 适用于需要更复杂对话历史的场景
     */
    public String reviewCodeWithMessages(String codeContent, String language) {
        // 检索相关规范
        List<String> relevantRules = knowledgeVectorService.searchSimilar(codeContent, 3);
        String knowledgeContext = relevantRules.isEmpty() ? "" 
                : "\n\n【相关编码规范】\n" + String.join("\n", relevantRules);

        // 手动构建消息列表
        List<Message> messages = new ArrayList<>();
        
        // 系统消息（可选，如果已经在 ChatClient 中设置了默认系统消息）
        messages.add(new SystemMessage("你是一位资深的代码审查专家..."));
        
        // 用户消息
        String userPrompt = String.format("语言：%s\n\n代码：\n%s%s", language, codeContent, knowledgeContext);
        messages.add(new UserMessage(userPrompt));

        // 使用消息列表调用
        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }
}
