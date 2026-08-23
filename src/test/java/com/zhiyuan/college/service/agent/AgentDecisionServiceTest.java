package com.zhiyuan.college.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentDecisionServiceTest {

    private final AgentToolRegistry registry = new AgentToolRegistry();
    private final AgentDecisionService service = new AgentDecisionService(
            null, new ObjectMapper(), registry, false);

    // --- P0 #9: getUserProfile 路由收紧 ---
    @Test
    void shouldNotTriggerGetUserProfile_whenUserDescribesScore() {
        AgentDecision d = service.decide("我620分想去北京上大学", List.of(), null);
        assertNotEquals(AgentToolNames.GET_USER_PROFILE, d.getAction());
    }

    @Test
    void shouldNotTriggerGetUserProfile_whenUserDescribesProvince() {
        AgentDecision d = service.decide("我是浙江考生想学计算机", List.of(), null);
        assertNotEquals(AgentToolNames.GET_USER_PROFILE, d.getAction());
    }

    @Test
    void shouldTriggerGetUserProfile_whenUserAsksProfile() {
        AgentDecision d = service.decide("我的画像是什么", List.of(), null);
        assertEquals(AgentToolNames.GET_USER_PROFILE, d.getAction());
    }

    // --- P0 #5: getSchoolDetailByName 收紧 ---
    @Test
    void shouldNotTriggerGetSchoolDetailByName_whenUserWantsRecommendation() {
        AgentDecision d = service.decide("看看能不能上浙江大学", List.of(), null);
        assertNotEquals(AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME, d.getAction());
    }

    @Test
    void shouldTriggerGetSchoolDetailByName_whenUserWantsDetail() {
        AgentDecision d = service.decide("查看浙江大学详情", List.of(), null);
        assertEquals(AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME, d.getAction());
    }

    // --- P0 #6: getSchoolDetail 收紧 ---
    @Test
    void shouldNotTriggerGetSchoolDetail_whenNoOrdinalReference() {
        AgentDecision d = service.decide("什么专业好就业", List.of(), null);
        assertNotEquals(AgentToolNames.GET_SCHOOL_DETAIL, d.getAction());
    }

    @Test
    void shouldTriggerGetSchoolDetail_whenOrdinalAndDetail() {
        AgentDecision d = service.decide("第一个学校详情", List.of(), null);
        assertEquals(AgentToolNames.GET_SCHOOL_DETAIL, d.getAction());
    }

    // --- P1 #2: 删除提示路由收紧 ---
    @Test
    void shouldNotTriggerDeletePrompt_whenPastTense() {
        AgentDecision d = service.decide("我刚把第3条志愿删除了", List.of(), null);
        assertEquals(AgentToolNames.REPLY, d.getAction());
        assertFalse(d.getReply().contains("确认删除第"));
    }

    // --- P1 #7: recommendMajors 关键词扩展 ---
    @Test
    void shouldTriggerRecommendMajors_forAIKeyword() {
        AgentDecision d = service.decide("推荐人工智能专业", List.of(), null);
        assertEquals(AgentToolNames.RECOMMEND_MAJORS, d.getAction());
    }

    @Test
    void shouldTriggerRecommendMajors_forFinanceKeyword() {
        AgentDecision d = service.decide("推荐金融专业", List.of(), null);
        assertEquals(AgentToolNames.RECOMMEND_MAJORS, d.getAction());
    }

    @Test
    void shouldUseMajorOverview_whenUserAsksAboutMajorProspectAndCurriculum() {
        AgentDecision d = service.decide("临床医学专业怎么样？就业前景和学习内容介绍一下", List.of(), null);
        assertEquals(AgentToolNames.GET_MAJOR_OVERVIEW, d.getAction());
        assertEquals("临床医学", d.getToolArgs().get("majorKeyword"));
    }

    @Test
    void shouldKeepRecommendation_whenUserExplicitlyRequestsMajorRecommendation() {
        AgentDecision d = service.decide("推荐临床医学专业", List.of(), null);
        assertEquals(AgentToolNames.RECOMMEND_MAJORS, d.getAction());
    }

    // --- 正向用例 ---
    @Test
    void shouldTriggerGetCurrentPlan_forViewCurrentPlan() {
        AgentDecision d = service.decide("查看当前志愿方案", List.of(), null);
        assertEquals(AgentToolNames.GET_CURRENT_PLAN, d.getAction());
    }

    @Test
    void shouldTriggerSavePlan() {
        AgentDecision d = service.decide("保存为“冲稳保方案”", List.of(), null);
        assertEquals(AgentToolNames.SAVE_PLAN, d.getAction());
    }

    @Test
    void shouldTriggerAddPlanItem() {
        AgentDecision d = service.decide("加入志愿单第2个", List.of(), null);
        assertEquals(AgentToolNames.ADD_PLAN_ITEM, d.getAction());
    }

    @Test
    void shouldTriggerRecommendSchools_forChongWenBao() {
        AgentDecision d = service.decide("请生成45志愿位冲稳保方案", List.of(), null);
        assertEquals(AgentToolNames.RECOMMEND_SCHOOLS, d.getAction());
    }

    // --- 审查补充：医学精确匹配 + 形容词清洗 + 修改画像排除 ---
    @Test
    void shouldExtractPreciseClinicalMedicineKeyword() {
        AgentDecision d = service.decide("推荐临床医学专业", List.of(), null);
        assertEquals(AgentToolNames.RECOMMEND_MAJORS, d.getAction());
        assertEquals("临床医学", d.getToolArgs().get("majorKeyword"));
    }

    @Test
    void shouldCleanAdjectiveFromMajorKeyword() {
        AgentDecision d = service.decide("推荐好的计算机专业", List.of(), null);
        assertEquals(AgentToolNames.RECOMMEND_MAJORS, d.getAction());
        assertEquals("计算机", d.getToolArgs().get("majorKeyword"));
    }

    @Test
    void shouldNotTriggerGetUserProfile_whenUserWantsToEdit() {
        AgentDecision d = service.decide("修改我的信息", List.of(), null);
        assertNotEquals(AgentToolNames.GET_USER_PROFILE, d.getAction());
    }
}
