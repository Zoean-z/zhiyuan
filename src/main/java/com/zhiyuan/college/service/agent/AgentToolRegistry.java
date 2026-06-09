package com.zhiyuan.college.service.agent;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgentToolRegistry {

    public Map<String, String> getToolDescriptions() {
        Map<String, String> tools = new LinkedHashMap<>();
        tools.put(AgentToolNames.GET_USER_PROFILE, "读取当前用户的分数、科类、省份等画像信息");
        tools.put(AgentToolNames.GET_CURRENT_PLAN, "读取当前用户最近保存的志愿方案摘要");
        tools.put(AgentToolNames.GET_SCHOOL_DETAIL, "查看最近推荐中的某个学校详情和专业列表，支持 selectionIndex，默认 1");
        tools.put(AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME, "按学校名查询院校详情和专业列表，必须带 universityName");
        tools.put(AgentToolNames.RECOMMEND_SCHOOLS, "基于当前用户画像生成学校推荐");
        tools.put(AgentToolNames.RECOMMEND_MAJORS, "基于当前用户画像和专业关键词生成专业推荐");
        tools.put(AgentToolNames.ADD_PLAN_ITEM, "把最近一轮推荐结果中的某一项加入当前志愿单，必须带 selectionIndex");
        tools.put(AgentToolNames.REMOVE_PLAN_ITEM, "从当前志愿单中移除某一项，必须带 selectionIndex，且要显式确认");
        tools.put(AgentToolNames.SAVE_PLAN, "把当前志愿单保存为指定方案名，必须带 planName");
        return tools;
    }

    public boolean supports(String toolName) {
        return getToolDescriptions().containsKey(toolName);
    }
}
