package com.agent.codereview.controller;

import com.agent.codereview.agent.UnifiedAgent;
import com.agent.codereview.dto.AssistantRequest;
import com.agent.codereview.dto.AssistantResponse;
import com.agent.codereview.entity.ReviewTask;
import com.agent.codereview.service.ReviewTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final Map<String, UnifiedAgent> agentMap = new HashMap<>();
    private final ReviewTaskService reviewTaskService;

    public AssistantController(List<UnifiedAgent> agents, ReviewTaskService reviewTaskService) {
        this.reviewTaskService = reviewTaskService;
        for (UnifiedAgent agent : agents) {
            agentMap.put(agent.getAgentName(), agent);
            log.info("注册 Agent: {} - {}", agent.getAgentName(), agent.getAgentDescription());
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<AssistantResponse> execute(@RequestBody @Valid AssistantRequest request) {
        log.info("收到智能助理请求: actionType={}, language={}", request.getActionType(), request.getLanguage());
        
        UnifiedAgent agent = agentMap.get(request.getActionType());
        if (agent == null) {
            return ResponseEntity.badRequest().body(AssistantResponse.error("不支持的 actionType: " + request.getActionType()));
        }
        
        ReviewTask task = reviewTaskService.createAssistantTask(
                request.getInput(),
                request.getLanguage(),
                request.getActionType(),
                request.getExtraContext()
        );
        
        return ResponseEntity.ok(AssistantResponse.success(String.valueOf(task.getId())));
    }

    @GetMapping("/agents")
    public ResponseEntity<List<AgentInfo>> listAgents() {
        List<AgentInfo> agents = agentMap.values().stream()
                .map(a -> new AgentInfo(a.getAgentName(), a.getAgentDescription()))
                .toList();
        return ResponseEntity.ok(agents);
    }

    public record AgentInfo(String name, String description) {}
}