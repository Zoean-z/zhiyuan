package com.zhiyuan.college.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.AgentChatTurnResponse;
import com.zhiyuan.college.model.dto.AgentMessageResponse;
import com.zhiyuan.college.model.entity.AgentMessage;
import com.zhiyuan.college.model.entity.UserAccount;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgentChatService {

    private static final int RECENT_MESSAGE_LIMIT = 12;
    private static final int MAX_TOOL_CALLS_PER_TURN = 1;

    private final AgentConversationService agentConversationService;
    private final AgentDecisionService agentDecisionService;
    private final AgentToolExecutor agentToolExecutor;
    private final ObjectMapper objectMapper;
    private final AgentReplyFormatter replyFormatter;

    public AgentChatService(AgentConversationService agentConversationService,
                            AgentDecisionService agentDecisionService,
                            AgentToolExecutor agentToolExecutor,
                            ObjectMapper objectMapper,
                            AgentReplyFormatter replyFormatter) {
        this.agentConversationService = agentConversationService;
        this.agentDecisionService = agentDecisionService;
        this.agentToolExecutor = agentToolExecutor;
        this.objectMapper = objectMapper;
        this.replyFormatter = replyFormatter;
    }

    public AgentChatTurnResponse sendMessage(Long userId,
                                             Long conversationId,
                                             String content,
                                             Long targetPlanId,
                                             UserAccount currentUser) {
        List<AgentMessageResponse> generated = new ArrayList<>();
        int toolCalls = 0;
        agentConversationService.appendMessage(
                userId,
                conversationId,
                AgentRoles.USER,
                AgentMessageTypes.TEXT,
                content.trim(),
                null,
                null
        );

        List<AgentMessage> recentMessages = agentConversationService.listRecentMessages(conversationId, RECENT_MESSAGE_LIMIT);
        AgentDecision decision = agentDecisionService.decide(content, recentMessages, currentUser);

        if (!AgentToolNames.REPLY.equals(decision.getAction())) {
            if (toolCalls >= MAX_TOOL_CALLS_PER_TURN) {
                AgentMessage assistantReply = agentConversationService.appendMessage(
                        userId,
                        conversationId,
                        AgentRoles.ASSISTANT,
                        AgentMessageTypes.TEXT,
                        "当前单轮对话的工具调用次数已达上限，请换一个问题继续。",
                        null,
                        null
                );
                generated.add(agentConversationService.toMessageResponse(assistantReply));
                return new AgentChatTurnResponse(conversationId, generated);
            }

            AgentMessage toolCall = agentConversationService.appendMessage(
                    userId,
                    conversationId,
                    AgentRoles.ASSISTANT,
                    AgentMessageTypes.TOOL_CALL,
                    decision.getReply() == null || decision.getReply().isBlank() ? "正在调用工具。" : decision.getReply(),
                    decision.getAction(),
                    toJson(decision.getToolArgs())
            );
            generated.add(agentConversationService.toMessageResponse(toolCall));
            toolCalls++;

            AgentToolResult toolResult;
            try {
                toolResult = agentToolExecutor.execute(userId, targetPlanId, decision.getAction(), decision.getToolArgs(), recentMessages);
            } catch (Exception ex) {
                toolResult = buildFailureResult(decision.getAction(), ex);
            }

            AgentMessage toolResultMessage = agentConversationService.appendMessage(
                    userId,
                    conversationId,
                    AgentRoles.TOOL,
                    AgentMessageTypes.TOOL_RESULT,
                    toolResult.getSummary(),
                    toolResult.getToolName(),
                    toolResult.getPayloadJson()
            );
            generated.add(agentConversationService.toMessageResponse(toolResultMessage));

            AgentMessage assistantFinal = agentConversationService.appendMessage(
                    userId,
                    conversationId,
                    AgentRoles.ASSISTANT,
                    AgentMessageTypes.TEXT,
                    replyFormatter.format(toolResult, currentUser),
                    null,
                    null
            );
            generated.add(agentConversationService.toMessageResponse(assistantFinal));
        } else {
            AgentMessage assistantReply = agentConversationService.appendMessage(
                    userId,
                    conversationId,
                    AgentRoles.ASSISTANT,
                    AgentMessageTypes.TEXT,
                    decision.getReply(),
                    null,
                    null
            );
            generated.add(agentConversationService.toMessageResponse(assistantReply));
        }

        return new AgentChatTurnResponse(conversationId, generated);
    }

    /**
     * SSE streaming variant of {@link #sendMessage}. The final reply is streamed chunk-by-chunk:
     * <ul>
     *   <li>tool_call progress → {@code {"type":"tool","data":{...}}}</li>
     *   <li>LLM fallback advice (empty data) → repeated {@code {"type":"chunk","data":"..."}}</li>
     *   <li>template markdown (real data) → single {@code {"type":"text","data":"..."}}</li>
     *   <li>end → {@code {"type":"done","data":null}}</li>
     * </ul>
     */
    public void streamMessage(Long userId,
                              Long conversationId,
                              String content,
                              Long targetPlanId,
                              UserAccount currentUser,
                              SseEmitter emitter) {
        try {
            agentConversationService.appendMessage(
                    userId, conversationId, AgentRoles.USER, AgentMessageTypes.TEXT, content.trim(), null, null);
            List<AgentMessage> recentMessages =
                    agentConversationService.listRecentMessages(conversationId, RECENT_MESSAGE_LIMIT);
            AgentDecision decision = agentDecisionService.decide(content, recentMessages, currentUser);

            if (AgentToolNames.REPLY.equals(decision.getAction())) {
                String reply = decision.getReply() == null || decision.getReply().isBlank()
                        ? "好的，请继续告诉我你的需求。" : decision.getReply();
                agentConversationService.appendMessage(
                        userId, conversationId, AgentRoles.ASSISTANT, AgentMessageTypes.TEXT, reply, null, null);
                sendEvent(emitter, "message",
                        Map.of("message", Map.of("role", "assistant", "messageType", "text", "content", reply)));
                sendDone(emitter);
                return;
            }

            // Tool path
            Map<String, Object> toolMeta = new LinkedHashMap<>();
            toolMeta.put("toolName", decision.getAction());
            toolMeta.put("content", decision.getReply());
            sendEvent(emitter, "tool_call", toolMeta);
            agentConversationService.appendMessage(
                    userId, conversationId, AgentRoles.ASSISTANT, AgentMessageTypes.TOOL_CALL,
                    decision.getReply() == null || decision.getReply().isBlank() ? "正在调用工具。" : decision.getReply(),
                    decision.getAction(),
                    toJson(decision.getToolArgs()));

            AgentToolResult toolResult;
            try {
                toolResult = agentToolExecutor.execute(
                        userId, targetPlanId, decision.getAction(), decision.getToolArgs(), recentMessages,
                        chunk -> sendEvent(emitter, "delta", Map.of("text", chunk)));
            } catch (Exception ex) {
                toolResult = buildFailureResult(decision.getAction(), ex);
            }
            agentConversationService.appendMessage(
                    userId, conversationId, AgentRoles.TOOL, AgentMessageTypes.TOOL_RESULT,
                    toolResult.getSummary(), toolResult.getToolName(), toolResult.getPayloadJson());

            String finalText = replyFormatter.format(toolResult, currentUser);
            boolean fallback = isFallbackResult(toolResult);
            if (!fallback) {
                // Real data → template markdown arrives at once
                sendEvent(emitter, "message",
                        Map.of("message", Map.of("role", "assistant", "messageType", "text", "content", finalText)));
            }
            agentConversationService.appendMessage(
                    userId, conversationId, AgentRoles.ASSISTANT, AgentMessageTypes.TEXT, finalText, null, null);
            sendDone(emitter);
        } catch (Exception ex) {
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignore) {
            }
        }
    }

    private boolean isFallbackResult(AgentToolResult toolResult) {
        if (toolResult == null || toolResult.getPayloadJson() == null || toolResult.getPayloadJson().isBlank()) {
            return false;
        }
        try {
            JsonNode payload = objectMapper.readTree(toolResult.getPayloadJson());
            return payload.path("fallback").asBoolean(false);
        } catch (Exception ex) {
            return false;
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String payload = data == null ? "null" : objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception ignore) {
        }
    }

    private void sendDone(SseEmitter emitter) {
        try {
            sendEvent(emitter, "done", null);
            emitter.complete();
        } catch (Exception ignore) {
        }
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize tool call payload", ex);
        }
    }

    private AgentToolResult buildFailureResult(String toolName, Exception ex) {
        String errorMessage = ex instanceof ResponseStatusException responseStatusException
                ? responseStatusException.getReason()
                : ex.getMessage();
        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = "执行工具时发生未知错误";
        }
        FailureDescriptor descriptor = classifyFailure(ex, errorMessage);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("toolName", toolName);
        payload.put("errorCategory", descriptor.category());
        payload.put("errorCode", descriptor.code());
        payload.put("errorMessage", errorMessage);
        return AgentToolResult.failure(toolName, descriptor.summary(), toJson(payload));
    }

    private FailureDescriptor classifyFailure(Exception ex, String errorMessage) {
        if (ex instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            if (isValidationError(errorMessage)) {
                return new FailureDescriptor(
                        "validation_error",
                        "invalid_tool_args",
                        "这次没有执行，因为参数不合法：" + errorMessage
                );
            }
            if (isContextMissingError(errorMessage)) {
                return new FailureDescriptor(
                        "context_missing",
                        resolveContextErrorCode(errorMessage),
                        "这次没有执行，因为当前上下文不足：" + errorMessage
                );
            }
        }
        return new FailureDescriptor(
                "tool_execution_error",
                "tool_execution_failed",
                "这次工具执行失败了：" + errorMessage
        );
    }

    private boolean isValidationError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        return errorMessage.contains("selectionIndex")
                || errorMessage.contains("majorKeyword")
                || errorMessage.contains("planName")
                || errorMessage.contains("Unsupported agent tool");
    }

    private boolean isContextMissingError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        return errorMessage.contains("Current plan is empty")
                || errorMessage.contains("Current user profile is incomplete")
                || errorMessage.contains("No recommendation item available")
                || errorMessage.contains("No school detail target available")
                || errorMessage.contains("Selected school detail is unavailable")
                || errorMessage.contains("out of range for current plan");
    }

    private String resolveContextErrorCode(String errorMessage) {
        if (errorMessage.contains("Current plan is empty")) {
            return "plan_missing";
        }
        if (errorMessage.contains("Current user profile is incomplete")) {
            return "profile_incomplete";
        }
        if (errorMessage.contains("No recommendation item available")) {
            return "recommendation_missing";
        }
        if (errorMessage.contains("No school detail target available")
                || errorMessage.contains("Selected school detail is unavailable")) {
            return "school_detail_target_missing";
        }
        if (errorMessage.contains("out of range for current plan")) {
            return "plan_selection_out_of_range";
        }
        return "context_missing";
    }

    private record FailureDescriptor(String category, String code, String summary) {
    }
}
