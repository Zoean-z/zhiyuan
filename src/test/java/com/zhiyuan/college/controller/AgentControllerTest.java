package com.zhiyuan.college.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.service.agent.AgentToolExecutor;
import com.zhiyuan.college.service.agent.AgentToolNames;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentToolExecutor agentToolExecutor;

    @Test
    void conversation_shouldBeCreatedListedAndLoaded() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");

        MvcResult createResult = mockMvc.perform(post("/api/agent/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"我的志愿助手\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("我的志愿助手"))
                .andExpect(jsonPath("$.messages").isArray())
                .andReturn();

        Long conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/agent/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].title").value("我的志愿助手"));

        mockMvc.perform(get("/api/agent/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.title").value("我的志愿助手"));
    }

    @Test
    void sendMessage_shouldInvokeReadOnlyToolsAndPersistMessages() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        Long conversationId = createConversation(token, "Agent 会话");

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"帮我看看我的分数和画像信息\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId))
                .andExpect(jsonPath("$.generatedMessages[0].messageType").value("tool_call"))
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("getUserProfile"))
                .andExpect(jsonPath("$.generatedMessages[1].messageType").value("tool_result"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.score").value(620))
                .andExpect(jsonPath("$.generatedMessages[2].messageType").value("text"));

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"看看我当前的志愿方案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("getCurrentPlan"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.hasPlan").exists());

        MvcResult detailResult = mockMvc.perform(get("/api/agent/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(8))
                .andExpect(jsonPath("$.messages").isArray())
                .andReturn();

        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        Assertions.assertEquals("user", detail.get("messages").get(0).get("role").asText());
        Assertions.assertTrue(detail.get("messages").size() >= 8);
    }

    @Test
    void sendMessage_shouldInvokeRecommendationTools() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        Long conversationId = createConversation(token, "Recommendation Agent");

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"帮我推荐学校\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("recommendSchools"))
                .andExpect(jsonPath("$.generatedMessages[1].toolName").value("recommendSchools"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.recommendationMode").value("SCHOOL_FIRST"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.topItems").isArray());

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"帮我推荐计算机专业\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("recommendMajors"))
                .andExpect(jsonPath("$.generatedMessages[0].payload.majorKeyword").value("计算机"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.recommendationMode").value("MAJOR_FIRST"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.topItems[0].majorName").exists());

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"看看第一个学校详情\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("getSchoolDetail"))
                .andExpect(jsonPath("$.generatedMessages[0].payload.selectionIndex").value(1))
                .andExpect(jsonPath("$.generatedMessages[1].toolName").value("getSchoolDetail"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.universityName").isNotEmpty())
                .andExpect(jsonPath("$.generatedMessages[1].payload.majors").isArray());

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"帮我看看浙江大学有哪些专业\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("getSchoolDetailByName"))
                .andExpect(jsonPath("$.generatedMessages[0].payload.universityName").value("浙江大学"))
                .andExpect(jsonPath("$.generatedMessages[1].toolName").value("getSchoolDetailByName"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.queryType").value("by_name"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.universityName").value("浙江大学"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.majors").isArray());
    }

    @Test
    void sendMessage_shouldAddLatestRecommendedItemToPlan() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        Long conversationId = createConversation(token, "Plan Agent");

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"帮我推荐学校\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"把第一个加入志愿单\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("addPlanItem"))
                .andExpect(jsonPath("$.generatedMessages[0].payload.selectionIndex").value(1))
                .andExpect(jsonPath("$.generatedMessages[1].toolName").value("addPlanItem"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.added").value(true))
                .andExpect(jsonPath("$.generatedMessages[1].payload.planId").isNumber())
                .andExpect(jsonPath("$.generatedMessages[1].payload.totalItems").value(1))
                .andExpect(jsonPath("$.generatedMessages[1].payload.selectedItem.universityName").isNotEmpty());

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"看看我当前的志愿方案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("getCurrentPlan"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.hasPlan").value(true))
                .andExpect(jsonPath("$.generatedMessages[1].payload.planName").value("当前方案草稿"));
    }

    @Test
    void sendMessage_shouldRequireConfirmBeforeRemovingAndAllowSavePlan() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        Long conversationId = createConversation(token, "Manage Plan Agent");

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"帮我推荐学校\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"把第一个加入志愿单\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"删除第一个志愿项\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].messageType").value("text"))
                .andExpect(jsonPath("$.generatedMessages[0].content").value(org.hamcrest.Matchers.containsString("确认删除第1个")));

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"确认删除第1个\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("removePlanItem"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.removed").value(true))
                .andExpect(jsonPath("$.generatedMessages[1].payload.totalItems").value(0));

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"保存为冲稳保方案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("savePlan"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.saved").value(true))
                .andExpect(jsonPath("$.generatedMessages[1].payload.planName").value("冲稳保方案"));

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"看看我当前的志愿方案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[1].payload.hasPlan").value(false));
    }

    @Test
    void sendMessage_shouldRejectStaleDeleteConfirmAndHandleToolFailureGracefully() throws Exception {
        String token = loginAndGetToken("testuser", "123456", 620, "PHYSICS", "浙江");
        Long conversationId = createConversation(token, "Hardening Agent");

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"确认删除第1个\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].messageType").value("text"))
                .andExpect(jsonPath("$.generatedMessages[0].content").value(org.hamcrest.Matchers.containsString("没有检测到最近一条待确认的删除请求")));

        mockMvc.perform(post("/api/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"把第一个加入志愿单\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedMessages[0].toolName").value("addPlanItem"))
                .andExpect(jsonPath("$.generatedMessages[1].toolName").value("addPlanItem"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.success").value(false))
                .andExpect(jsonPath("$.generatedMessages[1].payload.errorCategory").value("context_missing"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.errorCode").value("recommendation_missing"))
                .andExpect(jsonPath("$.generatedMessages[1].payload.errorMessage").isNotEmpty())
                .andExpect(jsonPath("$.generatedMessages[2].messageType").value("text"))
                .andExpect(jsonPath("$.generatedMessages[2].content").value(org.hamcrest.Matchers.containsString("当前上下文不足")));
    }

    @Test
    void toolExecutor_shouldRejectInvalidArgsWithStableValidationMessage() {
        ResponseStatusException majorKeywordError = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> agentToolExecutor.execute(1L, AgentToolNames.RECOMMEND_MAJORS, Map.of("majorKeyword", " "), List.of())
        );
        Assertions.assertTrue(majorKeywordError.getReason().contains("majorKeyword"));

        ResponseStatusException selectionIndexError = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> agentToolExecutor.execute(1L, AgentToolNames.GET_SCHOOL_DETAIL, Map.of("selectionIndex", 9), List.of())
        );
        Assertions.assertTrue(selectionIndexError.getReason().contains("selectionIndex"));
    }

    private Long createConversation(String token, String title) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/agent/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private String loginAndGetToken(String username,
                                    String password,
                                    Integer score,
                                    String subjectType,
                                    String examProvince) throws Exception {
        String loginRequest = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "score", score,
                "subjectType", subjectType,
                "examProvince", examProvince
        ));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
