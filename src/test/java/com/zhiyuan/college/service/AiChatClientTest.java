package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhiyuan.college.service.agent.AgentToolExecutor;
import com.zhiyuan.college.service.agent.AgentToolFacade;
import com.zhiyuan.college.service.agent.AgentToolNames;
import com.zhiyuan.college.service.agent.AgentToolRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class AiChatClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chat_shouldRetryTransientFailuresAndEventuallySucceed() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            int attempt = requestCount.incrementAndGet();
            if (attempt < 3) {
                writeJson(exchange, 503, "{\"error\":\"temporary unavailable\"}");
                return;
            }
            writeJson(exchange, 200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\\"action\\":\\"reply\\",\\"reply\\":\\"ok\\"}"
                          }
                        }
                      ]
                    }
                    """);
        });
        server.start();

        AiChatClient client = new AiChatClient(
                RestClient.builder(),
                "http://localhost:" + server.getAddress().getPort(),
                "test-key",
                "deepseek-v4-flash",
                3,
                0
        );

        String content = client.chat("system", "user", 0.1, true);

        assertEquals("{\"action\":\"reply\",\"reply\":\"ok\"}", content);
        assertEquals(3, requestCount.get());
    }

    @Test
    void chat_shouldNotRetryNonRetryableFailure() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            writeJson(exchange, 400, "{\"error\":\"bad request\"}");
        });
        server.start();

        AiChatClient client = new AiChatClient(
                RestClient.builder(),
                "http://localhost:" + server.getAddress().getPort(),
                "test-key",
                "deepseek-v4-flash",
                3,
                0
        );

        assertThrows(Exception.class, () -> client.chat("system", "user", 0.1, true));
        assertEquals(1, requestCount.get());
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }
}

class AgentToolExecutorValidationTest {

    @Test
    void execute_shouldRejectInvalidMajorKeyword() {
        AgentToolRegistry registry = mock(AgentToolRegistry.class);
        when(registry.supports(any())).thenReturn(true);
        AgentToolFacade facade = mock(AgentToolFacade.class);
        AgentToolExecutor executor = new AgentToolExecutor(registry, facade);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, AgentToolNames.RECOMMEND_MAJORS, Map.of("majorKeyword", " "), List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(String.valueOf(exception.getReason()).contains("majorKeyword"));
    }

    @Test
    void execute_shouldRejectOutOfRangeSelectionIndex() {
        AgentToolRegistry registry = mock(AgentToolRegistry.class);
        when(registry.supports(any())).thenReturn(true);
        AgentToolFacade facade = mock(AgentToolFacade.class);
        AgentToolExecutor executor = new AgentToolExecutor(registry, facade);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, AgentToolNames.GET_SCHOOL_DETAIL, Map.of("selectionIndex", 9), List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(String.valueOf(exception.getReason()).contains("selectionIndex"));
    }

    @Test
    void execute_shouldRejectInvalidPlanName() {
        AgentToolRegistry registry = mock(AgentToolRegistry.class);
        when(registry.supports(any())).thenReturn(true);
        AgentToolFacade facade = mock(AgentToolFacade.class);
        AgentToolExecutor executor = new AgentToolExecutor(registry, facade);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executor.execute(1L, AgentToolNames.SAVE_PLAN, Map.of("planName", "a"), List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(String.valueOf(exception.getReason()).contains("planName"));
    }
}
