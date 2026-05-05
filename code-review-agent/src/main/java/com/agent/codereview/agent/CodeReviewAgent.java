package com.agent.codereview.agent;

/**
 * 代码审查 Agent 接口
 * 定义每个 Agent 的基本行为
 */
public interface CodeReviewAgent {
    
    /**
     * 获取 Agent 名称
     */
    String getAgentName();
    
    /**
     * 获取 Agent 描述
     */
    String getAgentDescription();
    
    /**
     * 执行审查
     * @param codeContent 待审查代码
     * @param language 编程语言
     * @return 审查结果（Markdown 格式）
     */
    String review(String codeContent, String language);
}