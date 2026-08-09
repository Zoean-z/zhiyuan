package com.zhiyuan.college.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.entity.ApplicationPlan;
import com.zhiyuan.college.service.ApplicationPlanService;
import com.zhiyuan.college.service.RecommendationService;
import com.zhiyuan.college.service.SchoolDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AgentToolFacadeTest {

    @Test
    void getCurrentPlan_shouldRejectCorruptedStoredPlanInsteadOfTreatingItAsEmpty() {
        ApplicationPlanService applicationPlanService = mock(ApplicationPlanService.class);
        ApplicationPlan plan = new ApplicationPlan();
        plan.setId(8L);
        plan.setUserId(1L);
        plan.setPlanName("损坏方案");
        plan.setResultJson("{broken");
        when(applicationPlanService.findCurrentDraftEntity(1L)).thenReturn(plan);
        AgentToolFacade facade = new AgentToolFacade(
                mock(UserAccountMapper.class),
                applicationPlanService,
                mock(RecommendationService.class),
                mock(SchoolDetailService.class),
                new ObjectMapper()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> facade.getCurrentPlan(1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
