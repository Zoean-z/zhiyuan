package com.zhiyuan.college.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.mapper.AgentConversationMapper;
import com.zhiyuan.college.mapper.AgentMessageMapper;
import com.zhiyuan.college.model.dto.AgentConversationDetailResponse;
import com.zhiyuan.college.model.dto.AgentConversationRecordResponse;
import com.zhiyuan.college.model.dto.AgentMessageResponse;
import com.zhiyuan.college.model.entity.AgentConversation;
import com.zhiyuan.college.model.entity.AgentMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentConversationService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final AgentConversationMapper agentConversationMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final ObjectMapper objectMapper;

    public AgentConversationService(AgentConversationMapper agentConversationMapper,
                                    AgentMessageMapper agentMessageMapper,
                                    ObjectMapper objectMapper) {
        this.agentConversationMapper = agentConversationMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.objectMapper = objectMapper;
    }

    public AgentConversationDetailResponse createConversation(Long userId, String title) {
        AgentConversation conversation = new AgentConversation();
        conversation.setUserId(userId);
        conversation.setTitle(normalizeTitle(title));
        conversation.setStatus(STATUS_ACTIVE);
        conversation.setMessageCount(0);
        agentConversationMapper.insert(conversation);
        AgentConversation saved = agentConversationMapper.selectById(conversation.getId());
        return toDetailResponse(saved, List.of());
    }

    public List<AgentConversationRecordResponse> listByUser(Long userId) {
        LambdaQueryWrapper<AgentConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversation::getUserId, userId)
                .orderByDesc(AgentConversation::getUpdatedAt, AgentConversation::getId);
        return agentConversationMapper.selectList(wrapper).stream()
                .map(this::toRecordResponse)
                .toList();
    }

    public AgentConversationDetailResponse getById(Long userId, Long conversationId) {
        AgentConversation conversation = requireConversation(userId, conversationId);
        return toDetailResponse(conversation, listMessageEntities(conversationId));
    }

    public AgentConversation requireConversation(Long userId, Long conversationId) {
        AgentConversation conversation = agentConversationMapper.selectById(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
        return conversation;
    }

    public AgentMessage appendMessage(Long userId,
                                      Long conversationId,
                                      String role,
                                      String messageType,
                                      String content,
                                      String toolName,
                                      String payloadJson) {
        AgentConversation conversation = requireConversation(userId, conversationId);
        AgentMessage message = new AgentMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setToolName(toolName);
        message.setPayloadJson(payloadJson);
        agentMessageMapper.insert(message);

        LocalDateTime now = LocalDateTime.now();
        conversation.setLastMessageAt(now);
        Integer currentCount = conversation.getMessageCount() == null ? 0 : conversation.getMessageCount();
        conversation.setMessageCount(currentCount + 1);
        conversation.setUpdatedAt(now);
        agentConversationMapper.updateById(conversation);
        return agentMessageMapper.selectById(message.getId());
    }

    public List<AgentMessage> listRecentMessages(Long conversationId, int limit) {
        LambdaQueryWrapper<AgentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getConversationId, conversationId)
                .orderByDesc(AgentMessage::getCreatedAt, AgentMessage::getId)
                .last("LIMIT " + limit);
        List<AgentMessage> descending = agentMessageMapper.selectList(wrapper);
        java.util.Collections.reverse(descending);
        return descending;
    }

    public AgentMessageResponse toMessageResponse(AgentMessage message) {
        return new AgentMessageResponse(
                message.getId(),
                message.getRole(),
                message.getMessageType(),
                message.getContent(),
                message.getToolName(),
                parsePayload(message.getPayloadJson()),
                message.getCreatedAt()
        );
    }

    private AgentConversationRecordResponse toRecordResponse(AgentConversation conversation) {
        return new AgentConversationRecordResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getLastMessageAt(),
                conversation.getMessageCount(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private AgentConversationDetailResponse toDetailResponse(AgentConversation conversation,
                                                             List<AgentMessage> messages) {
        return new AgentConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getLastMessageAt(),
                conversation.getMessageCount(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages.stream().map(this::toMessageResponse).toList()
        );
    }

    private List<AgentMessage> listMessageEntities(Long conversationId) {
        LambdaQueryWrapper<AgentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getConversationId, conversationId)
                .orderByAsc(AgentMessage::getCreatedAt, AgentMessage::getId);
        return agentMessageMapper.selectList(wrapper);
    }

    private JsonNode parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(payloadJson);
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.trim().isBlank()) {
            return "新会话";
        }
        String trimmed = title.trim();
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }
}
