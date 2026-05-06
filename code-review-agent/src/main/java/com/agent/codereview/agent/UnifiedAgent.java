package com.agent.codereview.agent;

public interface UnifiedAgent {
    
    String execute(String codeContent, String language, String additionalContext);
    
    String getAgentName();
    
    String getAgentDescription();
}