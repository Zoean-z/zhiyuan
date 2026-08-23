package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhiyuan.college.model.dto.AiConnectionTestResponse;
import com.zhiyuan.college.model.dto.AiRuntimeConfigRequest;
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
import java.util.concurrent.CopyOnWriteArrayList;
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

    @Test
    void chat_shouldResolveLatestRuntimeConfigForEachNewRequest() throws Exception {
        List<String> requestBodies = new CopyOnWriteArrayList<>();
        List<String> authorizationHeaders = new CopyOnWriteArrayList<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            writeJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        AiRuntimeConfigService configService = mock(AiRuntimeConfigService.class);
        when(configService.resolve()).thenReturn(
                new AiRuntimeConfigService.ResolvedAiConfig("provider-a", baseUrl, "model-a", "key-a"),
                new AiRuntimeConfigService.ResolvedAiConfig("provider-b", baseUrl, "model-b", "key-b"));
        AiChatClient client = new AiChatClient(RestClient.builder(), configService, 1, 0, null, null);

        assertEquals("ok", client.chat("system", "first", 0.1, false));
        assertEquals("ok", client.chat("system", "second", 0.1, false));

        assertTrue(requestBodies.get(0).contains("\"model\":\"model-a\""));
        assertTrue(requestBodies.get(1).contains("\"model\":\"model-b\""));
        assertEquals(List.of("Bearer key-a", "Bearer key-b"), authorizationHeaders);
    }

    @Test
    void testConnection_shouldUseUnsavedFormConfigWithoutExposingProviderBody() throws Exception {
        List<String> requestBodies = new CopyOnWriteArrayList<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}");
        });
        server.start();

        AiRuntimeConfigRequest request = runtimeConfigRequest();
        AiRuntimeConfigService configService = mock(AiRuntimeConfigService.class);
        when(configService.resolveForTest(request)).thenReturn(new AiRuntimeConfigService.ResolvedAiConfig(
                "test-provider", "http://localhost:" + server.getAddress().getPort(), "test-model", "test-key"));
        AiChatClient client = new AiChatClient(RestClient.builder(), configService, 1, 0, null, null);

        AiConnectionTestResponse response = client.testConnection(request);

        assertTrue(response.available());
        assertEquals("test-provider", response.provider());
        assertEquals("test-model", response.model());
        assertTrue(requestBodies.get(0).contains("\"model\":\"test-model\""));
        assertFalse(requestBodies.get(0).contains("thinking"));
        assertFalse(requestBodies.get(0).contains("response_format"));
    }

    @Test
    void testConnection_shouldReturnSafeFailureForUpstreamRejection() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange ->
                writeJson(exchange, 401, "{\"error\":\"secret upstream diagnostic\"}"));
        server.start();

        AiRuntimeConfigRequest request = runtimeConfigRequest();
        AiRuntimeConfigService configService = mock(AiRuntimeConfigService.class);
        when(configService.resolveForTest(request)).thenReturn(new AiRuntimeConfigService.ResolvedAiConfig(
                "test-provider", "http://localhost:" + server.getAddress().getPort(), "test-model", "bad-key"));
        AiChatClient client = new AiChatClient(RestClient.builder(), configService, 1, 0, null, null);

        AiConnectionTestResponse response = client.testConnection(request);

        assertFalse(response.available());
        assertTrue(response.message().contains("认证失败（HTTP 401）"));
        assertFalse(response.message().contains("secret upstream diagnostic"));
    }

    private AiRuntimeConfigRequest runtimeConfigRequest() {
        AiRuntimeConfigRequest request = new AiRuntimeConfigRequest();
        request.setProvider("test-provider");
        request.setBaseUrl("https://unused.example.test");
        request.setModel("test-model");
        request.setApiKey("test-key");
        return request;
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
