package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeepSeekSearchClientTest {
    @Test void callsOfficialShapeAndDoesNotExposeUpstreamErrorBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/v1/tools/search", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"results\":[{\"url\":\"https://school.edu.cn/a\",\"title\":\"招生\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/v1/tools/fetch", exchange -> {
            byte[] body = "private upstream details".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var mapper = new ObjectMapper();
            var client = new DeepSeekSearchClient(mapper, null, "test-key", "http://127.0.0.1:" + server.getAddress().getPort(), HttpClient.newHttpClient());
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            assertEquals(1, client.search("招生".repeat(100), deadline).path("results").size());
            var sent = mapper.readTree(requestBody.get());
            assertTrue(sent.path("need_content").asBoolean());
            assertTrue(sent.path("query").asText().length() <= 128);
            var error = assertThrows(IllegalStateException.class, () -> client.fetch("https://school.edu.cn/a", deadline));
            assertTrue(error.getMessage().contains("凭据"));
            assertFalse(error.getMessage().contains("private"));
            assertThrows(IllegalStateException.class, () -> client.search("招生", System.nanoTime() - 1));
        } finally { server.stop(0); }
    }

    @Test void rejectsNonPublicAndCredentialBearingEvidenceLinks() {
        for (String url : new String[]{"file:///a", "http://127.0.0.1/a", "http://[::1]/", "http://localhost/", "https://user:pass@x.edu.cn/", "javascript:alert(1)"}) {
            assertFalse(DeepSeekSearchClient.isPublicUrl(url));
        }
        assertTrue(DeepSeekSearchClient.isPublicUrl("https://zsb.school.edu.cn/2026/plan"));
    }
}
