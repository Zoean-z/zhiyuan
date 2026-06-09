package com.zhiyuan.college.model.dto;

import java.util.List;

public class AgentChatTurnResponse {

    private Long conversationId;
    private List<AgentMessageResponse> generatedMessages;

    public AgentChatTurnResponse(Long conversationId, List<AgentMessageResponse> generatedMessages) {
        this.conversationId = conversationId;
        this.generatedMessages = generatedMessages;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public List<AgentMessageResponse> getGeneratedMessages() {
        return generatedMessages;
    }
}
