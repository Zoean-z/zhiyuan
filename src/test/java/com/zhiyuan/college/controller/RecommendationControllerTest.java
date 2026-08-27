package com.zhiyuan.college.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.security.JwtTokenService;
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

    @Autowired
    private JwtTokenService jwtTokenService;

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
                "province", "浙江",
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
                Assertions.assertFalse(item.get("strategyLabel").asText().isBlank());
                Assertions.assertTrue(item.get("riskScore").asInt() >= 0);
                Assertions.assertTrue(item.get("matchReasons").isArray());
                Assertions.assertFalse(item.get("explanation").asText().isBlank());
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
                "province", "浙江",
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
    void recommend_shouldUseHunanHistoryCompetitionCoverage() throws Exception {
        Integer simulatedMajorCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM major_admission_cutoff
                WHERE province = '湖南'
                  AND subject_type = '历史'
                  AND data_kind = 'SIMULATED'
                """, Integer.class);
        Integer cutoffMismatchCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM admission_cutoff cutoff
                WHERE cutoff.province = '湖南'
                  AND cutoff.subject_type = '历史'
                  AND cutoff.cutoff_score <> (
                    SELECT MIN(major_cutoff.cutoff_score)
                    FROM major_admission_cutoff major_cutoff
                    WHERE major_cutoff.university_id = cutoff.university_id
                      AND major_cutoff.admission_year = cutoff.admission_year
                      AND major_cutoff.province = cutoff.province
                      AND major_cutoff.subject_type = cutoff.subject_type
                      AND major_cutoff.data_kind = 'SIMULATED'
                  )
                """, Integer.class);
        Assertions.assertEquals(5, simulatedMajorCount);
        Assertions.assertEquals(0, cutoffMismatchCount);

        String token = loginAndGetToken("freshuser", "123456", 650, "HISTORY", "湖南");
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 650,
                "province", "湖南",
                "subjectType", "HISTORY"
        ));

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(1200, response.get("userRank").asInt());
        Assertions.assertEquals("SCHOOL_FIRST", response.get("recommendationMode").asText());
        Assertions.assertTrue(response.get("rush").size()
                + response.get("safe").size()
                + response.get("guarantee").size() >= 3);
        Assertions.assertFalse(response.get("summary").asText().contains("暂无历史类院校录取数据"));

        JsonNode provenanceItem = null;
        for (String group : new String[]{"rush", "safe", "guarantee"}) {
            if (!response.get(group).isEmpty()) {
                provenanceItem = response.get(group).get(0);
                break;
            }
        }
        Assertions.assertNotNull(provenanceItem);
        Assertions.assertEquals("SIMULATED", provenanceItem.get("dataKind").asText());
        Assertions.assertFalse(provenanceItem.get("calibrationSource").asText().isBlank());
        Assertions.assertFalse(provenanceItem.get("simulationRule").asText().isBlank());
    }

    @Test
    void publicCutoffApis_shouldExposeProvenanceAndKeepUncoveredProvinceEmpty() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/universities")
                        .param("examProvince", "湖南")
                        .param("subjectType", "HISTORY")
                        .param("withDataOnly", "true"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listResponse = objectMapper.readTree(listResult.getResponse().getContentAsString());
        Assertions.assertTrue(listResponse.get("items").size() >= 1);
        JsonNode listItem = listResponse.get("items").get(0);
        Assertions.assertEquals("SIMULATED", listItem.get("dataKind").asText());
        Assertions.assertFalse(listItem.get("calibrationSource").asText().isBlank());
        Assertions.assertFalse(listItem.get("simulationRule").asText().isBlank());

        MvcResult detailResult = mockMvc.perform(get("/api/universities/2")
                        .param("examProvince", "湖南")
                        .param("subjectType", "HISTORY"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detailResponse = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        JsonNode historyItem = detailResponse.get("cutoffHistory").get(0);
        Assertions.assertEquals("SIMULATED", historyItem.get("dataKind").asText());
        Assertions.assertFalse(historyItem.get("calibrationSource").asText().isBlank());
        Assertions.assertFalse(historyItem.get("simulationRule").asText().isBlank());
        JsonNode majorItem = detailResponse.get("majors").get(0);
        Assertions.assertEquals("SIMULATED", majorItem.get("dataKind").asText());
        Assertions.assertFalse(majorItem.get("calibrationSource").asText().isBlank());
        Assertions.assertFalse(majorItem.get("simulationRule").asText().isBlank());

        Long lawMajorId = jdbcTemplate.queryForObject(
                "SELECT id FROM major WHERE name = '法学' LIMIT 1", Long.class);
        mockMvc.perform(get("/api/majors/{majorId}/schools", lawMajorId)
                        .param("province", "湖南")
                        .param("subjectType", "HISTORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataKind").value("SIMULATED"))
                .andExpect(jsonPath("$[0].calibrationSource").isNotEmpty())
                .andExpect(jsonPath("$[0].simulationRule").isNotEmpty());

        mockMvc.perform(get("/api/majors/{majorId}/schools", lawMajorId)
                        .param("province", "江苏")
                        .param("subjectType", "HISTORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void recommend_shouldExposeMultipleSchoolTagsFor985University() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 660, "PHYSICS", "浙江");
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 660,
                "province", "浙江",
                "subjectType", "PHYSICS"
        ));

        mockMvc.perform(post("/api/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rush[0].universityName").value("浙江大学"))
                .andExpect(jsonPath("$.rush[0].universityId").value(1))
                .andExpect(jsonPath("$.rush[0].is985").value(true))
                .andExpect(jsonPath("$.rush[0].is211").value(true))
                .andExpect(jsonPath("$.rush[0].isDoubleFirstClass").value(true))
                .andExpect(jsonPath("$.rush[0].schoolTags[0]").value("985"))
                .andExpect(jsonPath("$.rush[0].schoolTags[1]").value("双一流"));
    }

    @Test
    void recommendMajor_shouldReturnSchoolAndMajorResults() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", "浙江",
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
                .andExpect(jsonPath("$.rush[0].strategyLabel").isNotEmpty())
                .andExpect(jsonPath("$.rush[0].matchReasons").isArray())
                .andExpect(jsonPath("$.rush[0].explanation").isNotEmpty())
                .andExpect(jsonPath("$.safe").isArray())
                .andExpect(jsonPath("$.guarantee").isArray());
    }

    @Test
    void schoolDetail_shouldReturnMajorsForSelectedSchool() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "娴欐睙");

        mockMvc.perform(get("/api/recommendations/schools/1/majors")
                        .header("Authorization", "Bearer " + token)
                        .param("province", "娴欐睙")
                        .param("subjectType", "PHYSICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universityId").value(1))
                .andExpect(jsonPath("$.schoolTags").isArray())
                .andExpect(jsonPath("$.majors").isArray())
                .andExpect(jsonPath("$.majors[0].majorName").isNotEmpty());
    }

    @Test
    void recommendMajor_shouldFallbackToScoreWhenRankMissing() throws Exception {
        String token = loginAndGetToken("freshuser", "123456", 620, "HISTORY", "浙江");
        JsonNode meta = fetchMeta();
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "score", 620,
                "province", "浙江",
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
    void majorSchools_shouldTranslateApiSubjectTypeToDatabaseValue() throws Exception {
        Long majorId = jdbcTemplate.queryForObject(
                "SELECT id FROM major WHERE name = ?",
                Long.class,
                "计算机科学与技术"
        );

        mockMvc.perform(get("/api/majors/" + majorId + "/schools")
                        .param("province", "浙江")
                        .param("subjectType", "PHYSICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].universityName").isNotEmpty())
                .andExpect(jsonPath("$[0].cutoffScore").isNumber());
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
    void recommendByTextTask_shouldSubmitAndQueryAsyncResult() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "我想报江苏211学校，稳一点"
                }
                """;

        MvcResult submitResult = mockMvc.perform(post("/api/recommendations/free-text/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").isNumber())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long taskId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("taskId").asLong();
        JsonNode task = awaitTextTask(token, taskId);
        Assertions.assertEquals("SUCCESS", task.get("status").asText());
        Assertions.assertTrue(task.get("durationMs").asLong() >= 0L);
        Assertions.assertTrue(task.get("resultCount").asInt() >= 1);
        Assertions.assertTrue(task.get("parsedRequirement").isObject());
        Assertions.assertTrue(task.get("result").isObject());
        Assertions.assertTrue(task.get("result").get("recommendations").isArray());
        Assertions.assertFalse(task.get("result").get("finalAdvice").asText().isBlank());
        Assertions.assertFalse(task.get("result").get("aiSummary").asText().isBlank());
    }

    @Test
    void recommendByTextTask_shouldOnlyAllowOwnerToQuery() throws Exception {
        String token1 = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String token2 = loginAndGetToken("freshuser", "123456", 610, "HISTORY", "浙江");
        String requestJson = """
                {
                  "requirementText": "我想报江苏211学校，稳一点"
                }
                """;

        MvcResult submitResult = mockMvc.perform(post("/api/recommendations/free-text/tasks")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        Long taskId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("taskId").asLong();
        awaitTextTask(token1, taskId);

        mockMvc.perform(get("/api/recommendations/free-text/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());
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
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].universityProvince").value("江苏"))
                .andExpect(jsonPath("$.recommendations[0].is985").value(false))
                .andExpect(jsonPath("$.recommendations[0].is211").value(true))
                .andExpect(jsonPath("$.recommendations[0].isDoubleFirstClass").value(true))
                .andExpect(jsonPath("$.recommendations[0].schoolTags[0]").value("双一流"));
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
    void recommendByText_shouldFilterOrdinarySchoolsUsingBooleanTags() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String requestJson = """
                {
                  "requirementText": "推荐一些浙江的普通学校"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertTrue(response.get("recommendations").isArray());
        Assertions.assertTrue(response.get("recommendations").size() >= 1);
        JsonNode first = response.get("recommendations").get(0);
        Assertions.assertFalse(first.get("is985").asBoolean());
        Assertions.assertFalse(first.get("is211").asBoolean());
        Assertions.assertFalse(first.get("isDoubleFirstClass").asBoolean());
        Assertions.assertEquals(0, first.get("schoolTags").size());
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
    void login_shouldAllowIncompleteProfileAndCompleteItAfterLogin() throws Exception {
        jdbcTemplate.update(
                "UPDATE users SET score = NULL, subject_type = NULL, exam_province = NULL WHERE username = ?",
                "freshuser");

        String requestJson = """
                {
                  "username": "freshuser",
                  "password": "123456"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        Assertions.assertTrue(loginResponse.get("score").isNull());
        Assertions.assertTrue(loginResponse.get("subjectType").isNull());
        Assertions.assertTrue(loginResponse.get("examProvince").isNull());

        mockMvc.perform(post("/api/auth/profile")
                        .header("Authorization", "Bearer " + loginResponse.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score": 618,
                                  "subjectType": "PHYSICS",
                                  "examProvince": "浙江"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(618))
                .andExpect(jsonPath("$.subjectType").value("PHYSICS"))
                .andExpect(jsonPath("$.examProvince").value("浙江"));

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + loginResponse.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("freshuser"))
                .andExpect(jsonPath("$.score").value(618))
                .andExpect(jsonPath("$.subjectType").value("PHYSICS"))
                .andExpect(jsonPath("$.examProvince").value("浙江"));

        Integer storedScore = jdbcTemplate.queryForObject(
                "SELECT score FROM users WHERE username = ?",
                Integer.class,
                "freshuser");
        Assertions.assertEquals(618, storedScore);
    }

    @Test
    void register_shouldCreateUserAndReturnToken() throws Exception {
        String username = "newuser_register";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456",
                                  "score": 612,
                                  "subjectType": "PHYSICS",
                                  "examProvince": "浙江"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.score").value(612))
                .andExpect(jsonPath("$.subjectType").value("PHYSICS"))
                .andExpect(jsonPath("$.examProvince").value("浙江"))
                .andExpect(jsonPath("$.role").value("USER"));

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        Assertions.assertEquals(1, userCount);
    }

    @Test
    void register_shouldRejectDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "123456",
                                  "score": 612,
                                  "subjectType": "PHYSICS",
                                  "examProvince": "浙江"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void logout_shouldInvalidateJwtToken() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "娴欐睙");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));

        mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoints_shouldRequireAdminRole() throws Exception {
        String userToken = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "娴欐睙");

        mockMvc.perform(get("/api/admin/universities")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void aiConfig_shouldRequireAdminEncryptKeyAndNeverReturnPlaintext() throws Exception {
        jdbcTemplate.update("DELETE FROM ai_runtime_config");
        String userToken = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        String adminToken = loginAndGetToken("adminuser", "123456", null, null, null);
        String plainKey = "sk-test-secret-9876";

        try {
            mockMvc.perform(get("/api/admin/ai-config")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/ai-config/test")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "provider":"openai-compatible",
                                      "baseUrl":"https://ai.example.test/v1",
                                      "model":"demo-model",
                                      "apiKey":"test-key",
                                      "clearApiKey":false
                                    }
                                    """))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/ai-config/test")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "provider":"openai-compatible",
                                      "baseUrl":"not-a-valid-url",
                                      "model":"demo-model",
                                      "apiKey":"test-key",
                                      "clearApiKey":false
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
            Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ai_runtime_config", Integer.class));

            mockMvc.perform(put("/api/admin/ai-config")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "provider":"openai-compatible",
                                      "baseUrl":"https://ai.example.test/v1/",
                                      "model":"demo-model",
                                      "apiKey":"%s",
                                      "clearApiKey":false
                                    }
                                    """.formatted(plainKey)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.baseUrl").value("https://ai.example.test/v1"))
                    .andExpect(jsonPath("$.model").value("demo-model"))
                    .andExpect(jsonPath("$.apiKeyConfigured").value(true))
                    .andExpect(jsonPath("$.apiKeyMasked").value("••••9876"))
                    .andExpect(jsonPath("$.apiKeySource").value("database"))
                    .andExpect(jsonPath("$.apiKey").doesNotExist());

            String encrypted = jdbcTemplate.queryForObject(
                    "SELECT encrypted_api_key FROM ai_runtime_config WHERE id = 1", String.class);
            Assertions.assertNotNull(encrypted);
            Assertions.assertFalse(encrypted.contains(plainKey));

            mockMvc.perform(get("/api/admin/ai-config")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apiKeyMasked").value("••••9876"))
                    .andExpect(jsonPath("$.apiKey").doesNotExist());
        } finally {
            jdbcTemplate.update("DELETE FROM ai_runtime_config");
        }
    }

    @Test
    void adminUserEndpoints_shouldUseRealUserDataAndEnforceAccountSettings() throws Exception {
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN', enabled = TRUE WHERE id = 3");
        jdbcTemplate.update("UPDATE users SET role = 'USER', enabled = TRUE WHERE id = 1");

        try {
            MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "adminuser",
                                      "password": "123456"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andReturn();
            String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString()).get("token").asText();
            String userToken = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("keyword", "testuser")
                            .param("role", "USER")
                            .param("enabled", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username").value("testuser"))
                    .andExpect(jsonPath("$[0].score").value(620))
                    .andExpect(jsonPath("$[0].examProvince").value("浙江"))
                    .andExpect(jsonPath("$[0].recommendationCount").isNumber())
                    .andExpect(jsonPath("$[0].planCount").isNumber())
                    .andExpect(jsonPath("$[0].conversationCount").isNumber())
                    .andExpect(jsonPath("$[0].password").doesNotExist());

            mockMvc.perform(get("/api/admin/users/overview")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").isNumber())
                    .andExpect(jsonPath("$.userCount").isNumber())
                    .andExpect(jsonPath("$.adminCount").isNumber())
                    .andExpect(jsonPath("$.disabledCount").isNumber());

            mockMvc.perform(put("/api/admin/users/1/settings")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "role": "USER",
                                      "enabled": false
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));

            mockMvc.perform(get("/api/history")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "testuser",
                                      "password": "123456"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(put("/api/admin/users/3/settings")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "role": "USER",
                                      "enabled": true
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        } finally {
            jdbcTemplate.update("UPDATE users SET role = 'USER', enabled = TRUE WHERE id = 1");
            jdbcTemplate.update("UPDATE users SET role = 'ADMIN', enabled = TRUE WHERE id = 3");
        }
    }

    @Test
    void adminEndpoints_shouldAllowAdminCrud() throws Exception {
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", "adminuser");
        String adminToken = loginAndGetToken("adminuser", "123456", 650, "PHYSICS", "娴欐睙");

        mockMvc.perform(get("/api/admin/universities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        MvcResult createResult = mockMvc.perform(post("/api/admin/universities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "鏂版祴璇曞ぇ瀛﹂櫌",
                                  "province": "娴欐睙",
                                  "tier": "鏅€?",
                                  "is985": false,
                                  "is211": false,
                                  "isDoubleFirstClass": false,
                                  "tags": "缁煎悎绫?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("鏂版祴璇曞ぇ瀛﹂櫌"))
                .andReturn();

        Long universityId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/admin/universities/" + universityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "鏂版祴璇曞ぇ瀛﹂櫌",
                                  "province": "姹熻嫃",
                                  "tier": "鍙屼竴娴?",
                                  "is985": false,
                                  "is211": false,
                                  "isDoubleFirstClass": true,
                                  "tags": "缁煎悎绫?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.province").value("姹熻嫃"))
                .andExpect(jsonPath("$.isDoubleFirstClass").value(true));
    }

    @org.junit.jupiter.api.Disabled
    @Test
    void adminMajorEndpoints_shouldAllowAdminCrud_legacyBrokenPayload() throws Exception {
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", "adminuser");
        String adminToken = loginAndGetToken("adminuser", "123456", 650, "PHYSICS", "娴欐睙");

        mockMvc.perform(get("/api/admin/majors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        MvcResult createResult = mockMvc.perform(post("/api/admin/majors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "鏁版嵁绉戝涓庡ぇ鏁版嵁鎶€鏈?",
                                  "category": "宸ョ",
                                  "degreeType": "宸ュ",
                                  "tags": "鐑棬,AI",
                                  "subjectRequirement": "鐗╃悊蹇呴€?,
                                  "description": "绠楁硶銆佺粺璁′笌鏁版嵁宸ョ▼鏂瑰悜"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.degreeType").isNotEmpty())
                .andReturn();

        Long majorId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/admin/majors/" + majorId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "鏁版嵁绉戝涓庡ぇ鏁版嵁鎶€鏈?",
                                  "category": "宸ョ",
                                  "degreeType": "宸ュ",
                                  "tags": "鐑棬,AI,鏁版嵁",
                                  "subjectRequirement": "鐗╃悊蹇呴€?,
                                  "description": "鏇存柊鍚庣殑涓撲笟鎻忚堪"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isNotEmpty())
                .andExpect(jsonPath("$.description").isNotEmpty());
    }

    @Test
    void adminMajorEndpoints_shouldAllowAdminCrud() throws Exception {
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", "adminuser");
        String adminToken = loginAndGetToken("adminuser", "123456", 650, "PHYSICS", "娴欐睙");

        mockMvc.perform(get("/api/admin/majors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        MvcResult createResult = mockMvc.perform(post("/api/admin/majors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Data Science",
                                  "category": "Engineering",
                                  "degreeType": "Bachelor",
                                  "tags": "hot,ai",
                                  "subjectRequirement": "PHYSICS_REQUIRED",
                                  "description": "focus on data and statistics"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Data Science"))
                .andExpect(jsonPath("$.degreeType").value("Bachelor"))
                .andReturn();

        Long majorId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/admin/majors/" + majorId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Data Science",
                                  "category": "Engineering",
                                  "degreeType": "Bachelor",
                                  "tags": "hot,ai,data",
                                  "subjectRequirement": "PHYSICS_REQUIRED",
                                  "description": "updated description"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").value("hot,ai,data"))
                .andExpect(jsonPath("$.description").value("updated description"));
    }

    @Test
    void adminAdmissionCutoffEndpoints_shouldAllowAdminCrud() throws Exception {
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", "adminuser");
        String adminToken = loginAndGetToken("adminuser", "123456", 650, "PHYSICS", "娴欐睙");

        mockMvc.perform(get("/api/admin/admission-cutoffs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("province", "娴欐睙")
                        .param("subjectType", "鐗╃悊"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        MvcResult createResult = mockMvc.perform(post("/api/admin/admission-cutoffs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "universityId": 1,
                                  "admissionYear": 2024,
                                  "province": "娴欐睙",
                                  "subjectType": "PHYSICS",
                                  "cutoffScore": 650,
                                  "minRank": 5500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.subjectType").value("物理"))
                .andReturn();

        Long cutoffId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/admin/admission-cutoffs/" + cutoffId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "universityId": 1,
                                  "admissionYear": 2024,
                                  "province": "姹熻嫃",
                                  "subjectType": "PHYSICS",
                                  "cutoffScore": 648,
                                  "minRank": 5800
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.province").value("姹熻嫃"))
                .andExpect(jsonPath("$.cutoffScore").value(648));
    }

    @Test
    void adminMajorAdmissionCutoffEndpoints_shouldAllowAdminCrud() throws Exception {
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", "adminuser");
        String adminToken = loginAndGetToken("adminuser", "123456", 650, "PHYSICS", "娴欐睙");

        mockMvc.perform(get("/api/admin/major-admission-cutoffs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("province", "娴欐睙")
                        .param("subjectType", "鐗╃悊")
                        .param("majorKeyword", "璁＄畻鏈?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        MvcResult createResult = mockMvc.perform(post("/api/admin/major-admission-cutoffs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "universityId": 1,
                                  "majorName": "鏁版嵁绉戝涓庡ぇ鏁版嵁鎶€鏈?",
                                  "admissionYear": 2024,
                                  "province": "娴欐睙",
                                  "subjectType": "PHYSICS",
                                  "cutoffScore": 646,
                                  "minRank": 6200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.majorName").value("鏁版嵁绉戝涓庡ぇ鏁版嵁鎶€鏈?"))
                .andReturn();

        Long cutoffId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/admin/major-admission-cutoffs/" + cutoffId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "universityId": 1,
                                  "majorName": "鏁版嵁绉戝涓庡ぇ鏁版嵁鎶€鏈?",
                                  "admissionYear": 2024,
                                  "province": "姹熻嫃",
                                  "subjectType": "PHYSICS",
                                  "cutoffScore": 645,
                                  "minRank": 6500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.province").value("姹熻嫃"))
                .andExpect(jsonPath("$.cutoffScore").value(645));
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

    @Test
    void currentPlanDraft_shouldSupportUpsertGetAndDelete() throws Exception {
        jdbcTemplate.update("DELETE FROM application_plan");
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");

        String draftJson = """
                {
                  "planName": "当前方案草稿",
                  "sourceType": "score",
                  "sourceQuery": "手动选择 1 条志愿结果",
                  "resultJson": "{\\"summary\\":\\"当前方案共选择 1 条志愿结果。\\",\\"safe\\":[{\\"universityName\\":\\"浙江大学\\",\\"strategy\\":\\"SAFE\\"}],\\"rush\\":[],\\"guarantee\\":[]}",
                  "aiSummary": "当前方案共选择 1 条志愿结果。"
                }
                """;

        mockMvc.perform(put("/api/plans/current")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("当前方案草稿"))
                .andExpect(jsonPath("$.sourceType").value("score"));

        mockMvc.perform(get("/api/plans/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("当前方案草稿"))
                .andExpect(jsonPath("$.resultJson").isNotEmpty());

        mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planName").value("当前方案草稿"));

        mockMvc.perform(delete("/api/plans/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/plans/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void recommendByText_shouldReturnExplanationAndAdvice() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "娴欐睙");
        String requestJson = """
                {
                  "requirementText": "我想报江苏211学校，稳一点"
                }
                """;

        mockMvc.perform(post("/api/recommendations/free-text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.finalAdvice").isNotEmpty())
                .andExpect(jsonPath("$.aiSummary").isNotEmpty());
    }

    private JsonNode fetchMeta() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/meta/options"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode awaitTextTask(String token, Long taskId) throws Exception {
        JsonNode lastResponse = null;
        for (int i = 0; i < 20; i++) {
            MvcResult result = mockMvc.perform(get("/api/recommendations/free-text/tasks/" + taskId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            lastResponse = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
            String status = lastResponse.get("status").asText();
            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                return lastResponse;
            }
            Thread.sleep(100);
        }
        Assertions.fail("Async recommendation task did not finish in time");
        return lastResponse;
    }

    private String loginAndGetToken(String username, String password, Integer score, String subjectType, String examProvince) throws Exception {
        if ("adminuser".equals(username)) {
            jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", username);
            return jwtTokenService.generateToken(3L, username, "ADMIN");
        }
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
