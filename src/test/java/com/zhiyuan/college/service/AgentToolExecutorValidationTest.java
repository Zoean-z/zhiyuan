package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhiyuan.college.model.entity.AgentMessage;
import com.zhiyuan.college.service.agent.AgentMessageTypes;
import com.zhiyuan.college.service.agent.AgentRoles;
import com.zhiyuan.college.service.agent.AgentToolExecutor;
import com.zhiyuan.college.service.agent.AgentToolFacade;
import com.zhiyuan.college.service.agent.AgentToolNames;
import com.zhiyuan.college.service.agent.AgentToolRegistry;
import com.zhiyuan.college.service.agent.AgentToolResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AgentToolExecutorValidationTest {

    private AgentToolRegistry registry;
    private AgentToolFacade facade;
    private AgentToolExecutor executor;

    @BeforeEach
    void setUp() {
        registry = mock(AgentToolRegistry.class);
        when(registry.supports(any())).thenReturn(true);
        facade = mock(AgentToolFacade.class);
        executor = new AgentToolExecutor(registry, facade);
    }

    @Test
    void execute_shouldRejectInvalidMajorKeyword() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, AgentToolNames.RECOMMEND_MAJORS, Map.of("majorKeyword", " "), List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(String.valueOf(exception.getReason()).contains("majorKeyword"));
    }

    @Test
    void execute_shouldRejectOutOfRangeSelectionIndex() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, AgentToolNames.GET_SCHOOL_DETAIL, Map.of("selectionIndex", 9), List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(String.valueOf(exception.getReason()).contains("selectionIndex"));
    }

    @Test
    void execute_shouldRejectInvalidPlanName() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, AgentToolNames.SAVE_PLAN, Map.of("planName", "a"), List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(String.valueOf(exception.getReason()).contains("planName"));
    }

    @Test
    void execute_shouldRejectModelInitiatedDeleteWithoutConfirmationChain() {
        List<AgentMessage> messages = List.of(
                message(AgentRoles.USER, AgentMessageTypes.TEXT, "删除当前志愿方案里的第一个")
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, 9L, AgentToolNames.REMOVE_PLAN_ITEM, Map.of("selectionIndex", 1), messages)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(String.valueOf(exception.getReason()).contains("explicit delete confirmation"));
        verify(facade, never()).removePlanItem(any(), any(), any());
    }

    @Test
    void execute_shouldRejectConfirmationForDifferentSelection() {
        List<AgentMessage> messages = List.of(
                message(AgentRoles.USER, AgentMessageTypes.TEXT, "删除当前志愿方案里的第一个"),
                message(AgentRoles.ASSISTANT, AgentMessageTypes.TEXT, "若确认删除当前志愿单中的第 1 个结果，请回复“确认删除第1个”。"),
                message(AgentRoles.USER, AgentMessageTypes.TEXT, "确认删除第2个")
        );

        assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, 9L, AgentToolNames.REMOVE_PLAN_ITEM, Map.of("selectionIndex", 2), messages)
        );

        verify(facade, never()).removePlanItem(any(), any(), any());
    }

    @Test
    void execute_shouldAllowDeleteOnlyAfterMatchingConfirmationChain() {
        Map<String, Object> args = Map.of("selectionIndex", 1);
        List<AgentMessage> messages = List.of(
                message(AgentRoles.USER, AgentMessageTypes.TEXT, "删除当前志愿方案里的第一个"),
                message(AgentRoles.ASSISTANT, AgentMessageTypes.TEXT, "若确认删除当前志愿单中的第 1 个结果，请回复“确认删除第1个”。"),
                message(AgentRoles.USER, AgentMessageTypes.TEXT, "确认删除第1个")
        );
        AgentToolResult expected = AgentToolResult.success(AgentToolNames.REMOVE_PLAN_ITEM, "removed", "{}");
        when(facade.removePlanItem(1L, 9L, args)).thenReturn(expected);

        AgentToolResult actual = executor.execute(1L, 9L, AgentToolNames.REMOVE_PLAN_ITEM, args, messages);

        assertSame(expected, actual);
        verify(facade).removePlanItem(eq(1L), eq(9L), eq(args));
    }

    private AgentMessage message(String role, String messageType, String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(role);
        message.setMessageType(messageType);
        message.setContent(content);
        return message;
    }
}
