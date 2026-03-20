package com.zhiyuan.college.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recommend_shouldReturnGroupedResults() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
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
                .andExpect(jsonPath("$.recommendationMode").value("SCHOOL_FIRST"))
                .andExpect(jsonPath("$.safe").isArray())
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    void recommend_shouldUseRankWhenRankMappingExists() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", meta.get("provinces").get(0).asText(),
                "subjectType", "PHYSICS"
        ));

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(26000, response.get("userRank").asInt());
        Assertions.assertTrue(response.get("safe").isArray());
        boolean matched = false;
        for (JsonNode item : response.get("safe")) {
            if (item.get("minRank").asInt() == 28000) {
                Assertions.assertEquals("RANK", item.get("recommendationBasis").asText());
                Assertions.assertEquals(26000, item.get("userRank").asInt());
                Assertions.assertEquals(2000, item.get("rankGap").asInt());
                matched = true;
                break;
            }
        }
        Assertions.assertTrue(matched);
    }

    @Test
    void recommend_shouldFallbackToScoreWhenRankMappingMissing() throws Exception {
        String token = loginAndGetToken("freshuser", "123456", 620, "HISTORY", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", meta.get("provinces").get(0).asText(),
                "subjectType", "HISTORY"
        ));

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertTrue(response.get("userRank").isNull());
        Assertions.assertEquals("SCHOOL_FIRST", response.get("recommendationMode").asText());
        Assertions.assertEquals(1, response.get("rush").size());
        Assertions.assertFalse(response.get("rush").get(0).get("universityName").asText().isBlank());
        Assertions.assertEquals("SCORE", response.get("rush").get(0).get("recommendationBasis").asText());
        Assertions.assertTrue(response.get("safe").isArray());
        Assertions.assertTrue(response.get("guarantee").isArray());
        Assertions.assertTrue(response.get("rush").size() + response.get("safe").size() + response.get("guarantee").size() >= 1);
    }

    @Test
    void recommendMajor_shouldReturnSchoolAndMajorResults() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", meta.get("provinces").get(0).asText(),
                "subjectType", "PHYSICS",
                "recommendationMode", "MAJOR_FIRST",
                "majorKeyword", "计算机"
        ));

        mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationMode").value("MAJOR_FIRST"))
                .andExpect(jsonPath("$.rush[0].universityName").isNotEmpty())
                .andExpect(jsonPath("$.rush[0].majorName").isNotEmpty())
                .andExpect(jsonPath("$.rush[0].recommendationMode").value("MAJOR_FIRST"))
                .andExpect(jsonPath("$.safe").isArray())
                .andExpect(jsonPath("$.guarantee").isArray());
    }

    @Test
    void recommendMajor_shouldFallbackToScoreWhenRankMissing() throws Exception {
        String token = loginAndGetToken("freshuser", "123456", 620, "HISTORY", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", meta.get("provinces").get(0).asText(),
                "subjectType", "HISTORY",
                "recommendationMode", "MAJOR_FIRST",
                "majorKeyword", "法学"
        ));

        mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationMode").value("MAJOR_FIRST"))
                .andExpect(jsonPath("$.userRank").isEmpty())
                .andExpect(jsonPath("$.rush[0].majorName").isNotEmpty())
                .andExpect(jsonPath("$.rush[0].recommendationBasis").value("SCORE"));
    }

    @Test
    void recommendMajor_shouldReturnTipsWhenResultsAreFew() throws Exception {
        String token = loginAndGetToken("freshuser", "123456", 620, "HISTORY", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", meta.get("provinces").get(0).asText(),
                "subjectType", "HISTORY",
                "recommendationMode", "MAJOR_FIRST",
                "majorKeyword", "法学"
        ));

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertTrue(response.get("tips").isArray());
        Assertions.assertTrue(response.get("tips").size() >= 2);
        Assertions.assertFalse(response.get("tips").get(0).asText().isBlank());
        Assertions.assertFalse(response.get("tips").get(1).asText().isBlank());
    }

    @Test
    void recommendMajor_shouldRequireMajorKeyword() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", meta.get("provinces").get(0).asText(),
                "subjectType", "PHYSICS",
                "recommendationMode", "MAJOR_FIRST"
        ));

        mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("majorKeyword is required when recommendationMode is MAJOR_FIRST"));
    }

    @Test
    void history_shouldSaveAndQueryCurrentUserOnly() throws Exception {
        jdbcTemplate.update("DELETE FROM recommendation_log");

        String token1 = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String token2 = loginAndGetToken("freshuser", "123456", 610, "HISTORY", "浙江");
        JsonNode meta = fetchMeta();

        String requestJson = """
                {
                  "score": 620,
                  "province": "%s",
                  "subjectType": "PHYSICS"
                }
                """.formatted(meta.get("provinces").get(0).asText());

        mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        MvcResult historyResult = mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode historyList = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        Assertions.assertTrue(historyList.isArray() && historyList.size() >= 1);
        JsonNode latest = historyList.get(0);
        Assertions.assertEquals("score", latest.get("queryType").asText());

        MvcResult emptyHistoryResult = mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode emptyHistory = objectMapper.readTree(emptyHistoryResult.getResponse().getContentAsString());
        Assertions.assertEquals(0, emptyHistory.size());

        long latestId = historyList.findValues("id").stream()
                .map(JsonNode::asLong)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        mockMvc.perform(get("/api/history/" + latestId)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(latestId))
                .andExpect(jsonPath("$.resultJson").isNotEmpty());

        mockMvc.perform(get("/api/history/" + latestId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/history/" + latestId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/history/" + latestId)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/history/" + latestId)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound());
    }

    @Test
    void options_shouldReturnMetaData() throws Exception {
        mockMvc.perform(get("/api/meta/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provinces").isArray())
                .andExpect(jsonPath("$.subjectTypes").isArray());
    }

    @Test
    void majorOptions_shouldReturnFuzzySuggestions() throws Exception {
        mockMvc.perform(get("/api/meta/major-options")
                        .param("keyword", "计算机")
                        .param("province", "浙江")
                        .param("subjectType", "PHYSICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").isNotEmpty());
    }

    @Test
    void recommend_shouldRejectInvalidRequest() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
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
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "我是浙江考生，物理类，求稳"
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
    void recommendByText_shouldParseStructuredConditionsForSchoolFirst() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "推荐一些江苏的211学校，稳一点"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.recommendationMode").value("SCHOOL_FIRST"))
                .andExpect(jsonPath("$.parsed.provinces[0]").value("江苏"))
                .andExpect(jsonPath("$.parsed.schoolLevels[0]").value("211"))
                .andExpect(jsonPath("$.parsed.riskPreference").value("稳"))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void recommendByText_shouldAutoRouteToMajorFirst() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "推荐一些计算机专业，保一点"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.recommendationMode").value("MAJOR_FIRST"))
                .andExpect(jsonPath("$.parsed.majorKeywords[0]").value("计算机"))
                .andExpect(jsonPath("$.parsed.normalizedMajors[0]").value("计算机科学与技术"))
                .andExpect(jsonPath("$.parsed.riskPreference").value("保"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].majorName").isNotEmpty());
    }

    @Test
    void recommendByText_shouldFilterBySchoolType() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "推荐一些江苏的师范类学校"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.recommendationMode").value("SCHOOL_FIRST"))
                .andExpect(jsonPath("$.parsed.provinces[0]").value("江苏"))
                .andExpect(jsonPath("$.parsed.schoolTypes[0]").value("师范类"))
                .andExpect(jsonPath("$.recommendations[0].universityName").isNotEmpty())
                .andExpect(jsonPath("$.recommendations[0].universityProvince").value("江苏"))
                .andExpect(jsonPath("$.recommendations[0].universityTags").value(org.hamcrest.Matchers.containsString("师范类")));
    }

    @Test
    void recommendByText_shouldHandleMedicalSchoolType() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "想找医学院校，稳一点"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.schoolTypes[0]").value("医药类"))
                .andExpect(jsonPath("$.parsed.riskPreference").value("稳"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].universityTags").value(org.hamcrest.Matchers.containsString("医药类")));
    }

    @Test
    void recommendByText_shouldReturnZeroResultTipsWithRelevantSuggestions() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "推荐一些北京的985师范类护理学专业，保一点"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(0, response.get("recommendations").size());
        Assertions.assertTrue(response.get("tips").isArray());
        Assertions.assertTrue(response.get("tips").size() >= 4);
        Assertions.assertFalse(response.get("tips").get(0).asText().isBlank());
        Assertions.assertFalse(response.get("tips").get(1).asText().isBlank());
    }

    @Test
    void recommendByText_shouldFallbackWhenMajorNormalizationMissing() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "推荐一些密码学专业，稳一点"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.recommendationMode").value("MAJOR_FIRST"))
                .andExpect(jsonPath("$.parsed.majorKeywords[0]").isNotEmpty())
                .andExpect(jsonPath("$.parsed.normalizedMajors").isArray())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void recommendByText_shouldCollectUnrecognizedPreferencesWithoutError() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "想去江苏，学校名气好一点，稳一点"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.provinces[0]").value("江苏"))
                .andExpect(jsonPath("$.parsed.riskPreference").value("稳"))
                .andExpect(jsonPath("$.parsed.unrecognizedPreferences[0]").isNotEmpty())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void recommendByText_shouldUpdateStoredScoreWhenTextContainsScore() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
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
        Assertions.assertEquals(630, loginResp.get("score").asInt());
    }

    @Test
    void finalAdvice_shouldReturnAiSummary() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
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
        jdbcTemplate.update(
                "UPDATE users SET score = NULL, subject_type = NULL, exam_province = NULL WHERE username = ?",
                "freshuser");

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

    @Test
    void plans_shouldSaveListAndQueryCurrentUserOnly() throws Exception {
        jdbcTemplate.update("DELETE FROM application_plan");

        String token1 = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "娴欐睙");
        String token2 = loginAndGetToken("freshuser", "123456", 610, "HISTORY", "娴欐睙");

        String requestJson = """
                {
                  "planName": "绋冲Ε鏂规",
                  "sourceType": "score",
                  "sourceQuery": "鍒嗘暟:620, 鐪佷唤:娴欐睙, 绉戠被:PHYSICS",
                  "resultJson": "{\\"summary\\":\\"AI鎬荤粨\\",\\"safe\\":[]}",
                  "aiSummary": "AI鎬荤粨"
                }
                """;

        MvcResult saveResult = mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("绋冲Ε鏂规"))
                .andExpect(jsonPath("$.sourceType").value("score"))
                .andExpect(jsonPath("$.aiSummary").value("AI鎬荤粨"))
                .andReturn();

        Long planId = objectMapper.readTree(saveResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(planId))
                .andExpect(jsonPath("$[0].planName").value("绋冲Ε鏂规"))
                .andExpect(jsonPath("$[0].sourceType").value("score"));

        MvcResult emptyResult = mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andReturn();
        Assertions.assertEquals(0, objectMapper.readTree(emptyResult.getResponse().getContentAsString()).size());

        mockMvc.perform(get("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId))
                .andExpect(jsonPath("$.resultJson").isNotEmpty())
                .andExpect(jsonPath("$.aiSummary").value("AI鎬荤粨"));

        mockMvc.perform(get("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound());
    }

    private JsonNode fetchMeta() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/meta/options"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String loginAndGetToken(String username, String password, Integer score, String subjectType, String examProvince) throws Exception {
        String payload = score == null && subjectType == null && examProvince == null
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
                  "score": %d,
                  "subjectType": "%s",
                  "examProvince": "%s"
                }
                """.formatted(username, password, score, subjectType, examProvince);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
