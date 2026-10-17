package com.zhiyuan.college.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.*;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.service.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdmissionResearchServiceTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final UniversityQueryService universities = mock(UniversityQueryService.class);
    private final DeepSeekSearchClient search = mock(DeepSeekSearchClient.class);
    private final AiChatClient ai = mock(AiChatClient.class);

    private UserAccount user() {
        UserAccount u = new UserAccount(); u.setScore(600); u.setSubjectTypeValue("PHYSICS");
        u.setExamProvince("湖南"); u.setUsername("do-not-send-name"); u.setPassword("do-not-send-password"); return u;
    }
    private AdmissionResearchContext request(Integer year) {
        return new AdmissionResearchContext(1L, "计算机", "湖南", "PHYSICS", 620, 10000,
                year, "本科批", List.of("化学", "生物"), LocalDate.of(2026, 6, 1));
    }
    private void school() throws Exception {
        when(universities.detail(1L, "湖南", "物理", 620, 10000)).thenReturn(mapper.readValue("""
                {"id":1,"name":"测试大学","province":"浙江","userRank":10000,"rankSource":"USER",
                 "probability":{"probability":96,"strategy":"GUARANTEE","admissionYear":2025},
                 "cutoffHistory":[{"admissionYear":2025,"cutoffScore":590,"minRank":20000,"dataKind":"SIMULATED"},
                                  {"admissionYear":2027,"cutoffScore":699,"minRank":1}],
                 "majors":[{"majorName":"计算机","admissionYear":2025,"planCount":10,"probability":{"probability":88}},
                           {"majorName":"未来专业","admissionYear":2027,"cutoffScore":699}]}
                """, UniversityDetailResponse.class));
    }

    @Test void providesFactsAndTrendEvidenceWithoutPrecisionOrPrivateProfile() throws Exception {
        school(); when(search.isConfigured()).thenReturn(true);
        when(search.search(anyString(), anyLong())).thenReturn(mapper.readTree("""
                {"results":[{"title":"招生公告","url":"https://zsb.school.edu.cn/plan","date":"2026-05-01T00:00:00Z","summary":"扩招并调整专业组","content":"官方正文"},
                {"url":"https://zsb.school.edu.cn/future","date":"2026-07-01T00:00:00Z"}]}
                """));
        when(ai.researchChat(anyString(), anyString(), anyInt())).thenReturn("录取概率约为 62%；请参考来源[1]");
        var result = new AdmissionResearchService(universities, search, ai, mapper, 3, 2).analyse(user(), request(2026), x -> {}, () -> false);
        ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
        verify(ai).researchChat(anyString(), context.capture(), eq(45000));
        String json = context.getValue();
        assertTrue(json.contains("SIMULATED")); assertTrue(json.contains("扩招并调整专业组"));
        assertTrue(json.contains("地区报考趋势")); assertTrue(json.contains("620"));
        assertFalse(json.contains("do-not-send")); assertFalse(json.contains("699"));
        assertFalse(json.contains("未来专业")); assertFalse(json.contains("\"probability\""));
        assertFalse(json.contains("/future")); assertFalse(result.report().contains("62%"));
        assertEquals(1, result.sources().size()); assertEquals(3, result.searchesCompleted());
        assertEquals("https://zsb.school.edu.cn/plan", result.sources().get(0).get("url"));
        assertTrue(result.report().contains("来源[1]"));
        assertFalse(result.report().contains("本次检索来源"));
        assertFalse(result.report().contains("https://zsb.school.edu.cn/plan"));
        verify(search, never()).fetch(anyString(), anyLong());
    }

    @Test void doesNotPretendToSearchWhenUnconfiguredAndHandlesModelFailure() throws Exception {
        school(); when(search.isConfigured()).thenReturn(false);
        when(ai.researchChat(anyString(), anyString(), anyInt())).thenThrow(new IllegalStateException("unavailable"));
        var result = new AdmissionResearchService(universities, search, ai, mapper, 8, 6).analyse(user(), request(2026), x -> {}, () -> false);
        assertEquals(0, result.searchesCompleted()); assertTrue(result.sources().isEmpty());
        assertTrue(result.report().contains("本次未联网")); assertTrue(result.report().contains("未生成AI研判"));
        verify(search, never()).search(anyString(), anyLong());
    }

    @Test void stopsBeforeNetworkAndRequiresExplicitTargetYear() {
        var service = new AdmissionResearchService(universities, search, ai, mapper, 8, 6);
        assertThrows(CancellationException.class, () -> service.analyse(user(), request(2026), x -> {}, () -> true));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.analyse(user(), request(null), x -> {}, () -> false));
        verifyNoInteractions(universities, search, ai);
    }

    @Test void reportsSearchFailureAndStillSuppliesLocalFactsToTheModel() throws Exception {
        school(); when(search.isConfigured()).thenReturn(true);
        when(search.search(anyString(), anyLong())).thenThrow(new IllegalStateException("搜索账户余额不足"));
        when(ai.researchChat(anyString(), anyString(), anyInt())).thenReturn("仅有本地数据，证据不足。");
        var result = new AdmissionResearchService(universities, search, ai, mapper, 8, 6).analyse(user(), request(2026), x -> {}, () -> false);
        assertTrue(result.report().contains("余额不足")); assertTrue(result.sources().isEmpty());
        verify(search, times(1)).search(anyString(), anyLong());
        verify(ai).researchChat(anyString(), contains("schoolFacts"), anyInt());
    }
}
