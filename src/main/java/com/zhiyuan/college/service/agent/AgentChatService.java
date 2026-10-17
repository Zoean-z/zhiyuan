package com.zhiyuan.college.service.agent;

import com.zhiyuan.college.util.AdmissionAssessmentDisplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.AgentChatTurnResponse;
import com.zhiyuan.college.model.dto.AgentMessageResponse;
import com.zhiyuan.college.model.entity.AgentMessage;
import com.zhiyuan.college.model.entity.UserAccount;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgentChatService {

    private static final int RECENT_MESSAGE_LIMIT = 12;
    private static final int MAX_TOOL_CALLS_PER_TURN = 1;
    // GPT-like pacing: 2-5 characters per chunk with a 16-28ms pause lands around
    // 150-200 chars/second, which reads as natural typing instead of a burst.
    private static final int STREAM_CHUNK_MIN_SIZE = 2;
    private static final int STREAM_CHUNK_MAX_SIZE = 5;
    private static final long STREAM_CHUNK_MIN_DELAY_MILLIS = 16L;
    private static final long STREAM_CHUNK_MAX_DELAY_MILLIS = 28L;

    private final AgentConversationService agentConversationService;
    private final AgentDecisionService agentDecisionService;
    private final AgentToolExecutor agentToolExecutor;
    private final ObjectMapper objectMapper;
    private final AgentReplyFormatter replyFormatter;
    private final AgentFailureRewriteService failureRewriteService;

    @org.springframework.beans.factory.annotation.Autowired
    private AdmissionResearchService admissionResearchService;

    /** Explicit read-only mode; never routes through business tool selection. */
    public AgentChatTurnResponse researchMessage(Long userId, Long conversationId, String content,
            UserAccount user, com.zhiyuan.college.model.dto.AdmissionResearchContext context, SseEmitter emitter) {
        // appendMessage verifies conversation ownership before any external work.
        agentConversationService.appendMessage(userId, conversationId, AgentRoles.USER,
                AgentMessageTypes.TEXT, content.trim(), null, null);
        AtomicBoolean stopped = new AtomicBoolean(false);
        Thread worker = Thread.currentThread();
        if (emitter != null) {
            emitter.onCompletion(() -> stopped.set(true));
            emitter.onTimeout(() -> { stopped.set(true); worker.interrupt(); });
            emitter.onError(ex -> { stopped.set(true); worker.interrupt(); });
        }
        var heartbeat = emitter == null ? null : java.util.concurrent.Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "admission-research-heartbeat"); thread.setDaemon(true); return thread;
        });
        if (heartbeat != null) heartbeat.scheduleAtFixedRate(() -> {
            if (stopped.get()) return;
            try { emitter.send(SseEmitter.event().name("ping").data(Map.of(), MediaType.APPLICATION_JSON)); }
            catch (Exception ex) { stopped.set(true); worker.interrupt(); }
        }, 2, 2, java.util.concurrent.TimeUnit.SECONDS);
        List<AgentMessageResponse> generated = new ArrayList<>();
        try {
            var call = agentConversationService.appendMessage(userId, conversationId, AgentRoles.ASSISTANT,
                    AgentMessageTypes.TOOL_CALL, "正在整理院校资料并联网分析", "admissionResearch", null);
            generated.add(agentConversationService.toMessageResponse(call));
            var result = admissionResearchService.analyse(user, context, step -> {
                if (stopped.get()) throw new java.util.concurrent.CancellationException();
                if (emitter != null) {
                    try { emitter.send(SseEmitter.event().name("tool_call").data(
                            Map.of("toolName", "admissionResearch", "content", step), MediaType.APPLICATION_JSON)); }
                    catch (Exception ex) { stopped.set(true); throw new java.util.concurrent.CancellationException(); }
                }
            }, stopped::get);
            if (stopped.get()) throw new java.util.concurrent.CancellationException();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sources", result.sources()); payload.put("gaps", result.gaps());
            payload.put("searchesCompleted", result.searchesCompleted());
            var evidence = agentConversationService.appendMessage(userId, conversationId, AgentRoles.TOOL,
                    AgentMessageTypes.TOOL_RESULT, "资料整理完成", "admissionResearch", toJson(payload));
            generated.add(agentConversationService.toMessageResponse(evidence));
            var answer = agentConversationService.appendMessage(userId, conversationId, AgentRoles.ASSISTANT,
                    AgentMessageTypes.TEXT, result.report(), null, null);
            generated.add(agentConversationService.toMessageResponse(answer));
            if (emitter != null && !stopped.get()) {
                sendEvent(emitter, "tool_result", Map.of("toolName", "admissionResearch", "content", "资料整理完成", "payload", payload));
                sendEvent(emitter, "message", Map.of("message", Map.of("role", "assistant", "messageType", "text", "content", result.report())));
                sendDone(emitter);
            }
            return new AgentChatTurnResponse(conversationId, generated);
        } catch (java.util.concurrent.CancellationException ex) {
            if (emitter != null) emitter.complete();
            return new AgentChatTurnResponse(conversationId, generated);
        } catch (RuntimeException ex) {
            if (emitter == null) throw ex;
            String message = ex instanceof ResponseStatusException response ? response.getReason() : "院校分析暂未完成，请稍后重试";
            var answer = agentConversationService.appendMessage(userId, conversationId, AgentRoles.ASSISTANT,
                    AgentMessageTypes.TEXT, message, null, null);
            sendEvent(emitter, "message", Map.of("message", Map.of("role", "assistant", "messageType", "text", "content", message)));
            sendDone(emitter);
            return new AgentChatTurnResponse(conversationId, List.of(agentConversationService.toMessageResponse(answer)));
        } finally {
            if (heartbeat != null) heartbeat.shutdownNow();
        }
    }


    public AgentChatService(AgentConversationService agentConversationService,
                            AgentDecisionService agentDecisionService,
                            AgentToolExecutor agentToolExecutor,
                            ObjectMapper objectMapper,
                            AgentReplyFormatter replyFormatter,
                            AgentFailureRewriteService failureRewriteService) {
        this.agentConversationService = agentConversationService;
        this.agentDecisionService = agentDecisionService;
        this.agentToolExecutor = agentToolExecutor;
        this.objectMapper = objectMapper;
        this.replyFormatter = replyFormatter;
        this.failureRewriteService = failureRewriteService;
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
                    userId, conversationId, AgentRoles.ASSISTANT, AgentMessageTypes.TEXT,
                    finalTextForUser(content, decision, toolResult, currentUser), null, null);
            generated.add(agentConversationService.toMessageResponse(assistantFinal));
        } else {
            AgentMessage assistantReply = agentConversationService.appendMessage(
                    userId,
                    conversationId,
                    AgentRoles.ASSISTANT,
                    AgentMessageTypes.TEXT,
                    AdmissionAssessmentDisplay.text(decision.getReply()),
                    null,
                    null
            );
            generated.add(agentConversationService.toMessageResponse(assistantReply));
        }

        return new AgentChatTurnResponse(conversationId, generated);
    }

    /**
     * SSE streaming variant of {@link #sendMessage}. Events use named SSE types:
     * <ul>
     *   <li>{@code tool_call} → {@code {"toolName":..., "content":...}} progress marker</li>
     *   <li>{@code tool_result} → {@code {"toolName":..., "content":..., "payload":...}}</li>
     *   <li>{@code delta} → {@code {"text":"..."}} progressive text chunks (GPT-style typing;
     *       real LLM deltas when a model is configured, otherwise simulated pacing)</li>
     *   <li>{@code message} → complete final message, only when no delta was sent at all</li>
     *   <li>{@code done} → end of turn</li>
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
                streamTemplateText(emitter, AdmissionAssessmentDisplay.text(reply));
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
            AtomicBoolean streamedText = new AtomicBoolean(false);
            try {
                toolResult = agentToolExecutor.execute(
                        userId, targetPlanId, decision.getAction(), decision.getToolArgs(), recentMessages,
                        chunk -> {
                            if (chunk != null && !chunk.isEmpty()) {
                                streamedText.set(true);
                                sendEvent(emitter, "delta", Map.of("text", chunk));
                            }
                        });
            } catch (Exception ex) {
                toolResult = buildFailureResult(decision.getAction(), ex);
            }
            agentConversationService.appendMessage(
                    userId, conversationId, AgentRoles.TOOL, AgentMessageTypes.TOOL_RESULT,
                    toolResult.getSummary(), toolResult.getToolName(), toolResult.getPayloadJson());
            sendToolResult(emitter, toolResult);

            String finalText = finalTextForUser(content, decision, toolResult, currentUser);
            // If the executor/LLM already streamed text (fallback advice with a live model),
            // finalText is the same content reformatted — re-streaming it would duplicate the
            // answer. Otherwise simulate GPT-style token streaming for the final text.
            if (!streamedText.get()) {
                streamedText.set(streamTemplateText(emitter, finalText));
            }
            // The browser turns deltas into one live bubble. Sending the same final text again
            // as a message event made some clients render two identical answers. Only send a
            // complete message when no text was streamed at all (for example an empty reply).
            if (!streamedText.get()) {
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

    /**
     * Simulated token streaming for instantly generated text: slices the final markdown
     * into randomized small chunks emitted as {@code delta} events with short, irregular
     * pauses so the UI shows a GPT-like typing effect even when the answer was produced
     * locally in one shot.
     */
    private boolean streamTemplateText(SseEmitter emitter, String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int index = 0;
        while (index < text.length()) {
            int chunkSize = Math.min(
                    random.nextInt(STREAM_CHUNK_MIN_SIZE, STREAM_CHUNK_MAX_SIZE + 1),
                    text.length() - index);
            sendEvent(emitter, "delta", Map.of("text", text.substring(index, index + chunkSize)));
            index += chunkSize;
            if (index < text.length()) {
                try {
                    Thread.sleep(random.nextLong(STREAM_CHUNK_MIN_DELAY_MILLIS, STREAM_CHUNK_MAX_DELAY_MILLIS + 1));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * 工具失败时的最终话术（产品指定设计，默认开）：
     * 只读查询类工具硬失败（如学校未收录）→ 交给 LLM 用通用知识改写成有帮助的回复，
     * 替代原来的红色报错；改写不可用/关闭时回落 replyFormatter 的既有话术。
     * 成功结果与志愿单增/删/存类失败维持既有 formatter（后者涉及用户数据状态，须确定性引导）。
     */
    private String finalTextForUser(String userMessage, AgentDecision decision, AgentToolResult toolResult, UserAccount user) {
        String fallbackText = replyFormatter.format(toolResult, user);
        if (failureRewriteService.isRewritable(toolResult.getToolName()) && isFailurePayload(toolResult)) {
            String rewritten = failureRewriteService.rewrite(
                    userMessage,
                    toolResult.getToolName(),
                    toJson(decision.getToolArgs()),
                    extractFailureField(toolResult, "errorMessage"));
            if (rewritten != null && !rewritten.isBlank()) {
                return AdmissionAssessmentDisplay.text(rewritten);
            }
        }
        return AdmissionAssessmentDisplay.text(fallbackText);
    }

    private boolean isFailurePayload(AgentToolResult toolResult) {
        try {
            return !objectMapper.readTree(payloadOrEmpty(toolResult)).path("success").asBoolean(true);
        } catch (Exception ex) {
            return false;
        }
    }

    private String extractFailureField(AgentToolResult toolResult, String field) {
        try {
            return objectMapper.readTree(payloadOrEmpty(toolResult)).path(field).asText("");
        } catch (Exception ex) {
            return "";
        }
    }

    private String payloadOrEmpty(AgentToolResult toolResult) {
        return toolResult.getPayloadJson() == null || toolResult.getPayloadJson().isBlank()
                ? "{}" : toolResult.getPayloadJson();
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String payload = data == null ? "null" : objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception ignore) {
        }
    }

    private void sendToolResult(SseEmitter emitter, AgentToolResult toolResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("toolName", toolResult.getToolName());
        data.put("content", toolResult.getSummary());
        data.put("payload", parseEventPayload(toolResult.getPayloadJson()));
        sendEvent(emitter, "tool_result", data);
    }

    private Object parseEventPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception ignore) {
            return null;
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
