package com.zhiyuan.college.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recommend_shouldReturnGroupedResults() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620);
        JsonNode meta = fetchMeta();

        String requestJson = """
                {
                  "score": 620,
                  "province": "%s",
                  "subjectType": "PHYSICS"
                }
                """.formatted(meta.get("provinces").get(0).asText());

        mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.safe").isArray())
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    void options_shouldReturnMetaData() throws Exception {
        mockMvc.perform(get("/api/meta/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provinces").isArray())
                .andExpect(jsonPath("$.subjectTypes").isArray());
    }

    @Test
    void recommend_shouldRejectInvalidRequest() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620);
        String requestJson = """
                {
                  "score": 900,
                  "province": "",
                  "subjectType": "PHYSICS"
                }
                """;

        mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recommendByText_shouldUseStoredScoreWhenTextHasNoScore() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620);
        String requestJson = """
                {
                  "requirementText": "我想在浙江上大学，物理类，求稳"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.score").value(620))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void recommendByText_shouldUpdateStoredScoreWhenTextContainsScore() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620);
        String requestJson = """
                {
                  "requirementText": "我630分，想在浙江上大学，物理类"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.score").value(630));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginResp = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(630, loginResp.get("score").asInt());
    }

    @Test
    void finalAdvice_shouldReturnAiSummary() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620);
        JsonNode meta = fetchMeta();
        String province = meta.get("provinces").get(0).asText();
        String requestJson = """
                {
                  "score": 620,
                  "province": "%s",
                  "subjectType": "PHYSICS",
                  "strategy": "SAFE",
                  "preferredUniversities": []
                }
                """.formatted(province);

        mockMvc.perform(post("/api/recommendations/final-advice")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedStrategy").value("SAFE"))
                .andExpect(jsonPath("$.recommendedUniversities").isArray())
                .andExpect(jsonPath("$.finalAdvice").isNotEmpty())
                .andExpect(jsonPath("$.aiSummary").isNotEmpty());
    }

    @Test
    void login_shouldRequireScoreAtFirstLogin() throws Exception {
        String requestJson = """
                {
                  "username": "freshuser",
                  "password": "123456"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    private JsonNode fetchMeta() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/meta/options"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String loginAndGetToken(String username, String password, Integer score) throws Exception {
        String payload = score == null
                ? """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password)
                : """
                {
                  "username": "%s",
                  "password": "%s",
                  "score": %d
                }
                """.formatted(username, password, score);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
