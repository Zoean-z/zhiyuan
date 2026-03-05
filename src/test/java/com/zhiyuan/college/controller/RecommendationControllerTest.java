package com.zhiyuan.college.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recommend_shouldReturnGroupedResults() throws Exception {
        String requestJson = """
                {
                  "score": 620,
                  "province": "浙江",
                  "subjectType": "物理"
                }
                """;

        mockMvc.perform(post("/api/recommendations")
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
        String requestJson = """
                {
                  "score": 900,
                  "province": "",
                  "subjectType": "物理"
                }
                """;
        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recommendByText_shouldReturnParsedAndRecommendations() throws Exception {
        String requestJson = """
                {
                  "requirementText": "我620分，想在浙江上大学，偏稳妥一点，物理类。"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.score").value(620))
                .andExpect(jsonPath("$.parsed.candidateProvince").value("浙江"))
                .andExpect(jsonPath("$.parsed.strategy").value("SAFE"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.finalAdvice").isNotEmpty())
                .andExpect(jsonPath("$.aiSummary").isNotEmpty());
    }

    @Test
    void finalAdvice_shouldReturnAiSummary() throws Exception {
        String requestJson = """
                {
                  "score": 620,
                  "province": "浙江",
                  "subjectType": "物理",
                  "strategy": "稳定",
                  "preferredUniversities": ["宁波大学"]
                }
                """;

        mockMvc.perform(post("/api/recommendations/final-advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedStrategy").value("SAFE"))
                .andExpect(jsonPath("$.recommendedUniversities").isArray())
                .andExpect(jsonPath("$.finalAdvice").isNotEmpty())
                .andExpect(jsonPath("$.aiSummary").isNotEmpty());
    }
}
