package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.AgentChatTurnResponse;
import com.zhiyuan.college.model.dto.AgentConversationCreateRequest;
import com.zhiyuan.college.model.dto.AgentConversationDetailResponse;
import com.zhiyuan.college.model.dto.AgentConversationRecordResponse;
import com.zhiyuan.college.model.dto.AgentMessageCreateRequest;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.security.UserContext;
import com.zhiyuan.college.service.agent.AgentChatService;
import com.zhiyuan.college.service.agent.AgentConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/agent/conversations")
public class AgentController {

    private final AgentConversationService agentConversationService;
    private final AgentChatService agentChatService;

    public AgentController(AgentConversationService agentConversationService,
                           AgentChatService agentChatService) {
        this.agentConversationService = agentConversationService;
        this.agentChatService = agentChatService;
    }

    @PostMapping
    public AgentConversationDetailResponse createConversation(@RequestBody(required = false) AgentConversationCreateRequest request) {
        return agentConversationService.createConversation(
                currentUserId(),
                request == null ? null : request.getTitle()
        );
    }

    @GetMapping
    public List<AgentConversationRecordResponse> listConversations() {
        return agentConversationService.listByUser(currentUserId());
    }

    @GetMapping("/{id}")
    public AgentConversationDetailResponse getConversation(@PathVariable("id") Long id) {
        return agentConversationService.getById(currentUserId(), id);
    }

    @PostMapping("/{id}/messages")
    public AgentChatTurnResponse sendMessage(@PathVariable("id") Long id,
                                             @Valid @RequestBody AgentMessageCreateRequest request) {
        return agentChatService.sendMessage(currentUserId(), id, request.getContent(), request.getPlanId(), currentUser());
    }

    private Long currentUserId() {
        UserAccount user = currentUser();
        return user.getId();
    }

    private UserAccount currentUser() {
        UserAccount user = UserContext.get();
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user;
    }
}
