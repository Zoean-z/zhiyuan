package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

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
