package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhiyuan.college.config.RecommendationScoringProperties;
import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.mapper.UniversityMapper;
import com.zhiyuan.college.model.dto.ProbabilityBatchRequest;
import com.zhiyuan.college.model.dto.ProbabilityBreakdownResponse;
import com.zhiyuan.college.model.dto.ProbabilityRequest;
import com.zhiyuan.college.model.entity.AdmissionCutoff;
import com.zhiyuan.college.model.entity.MajorAdmissionCutoff;
import com.zhiyuan.college.model.entity.University;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.ScoreRankMappingService.RankResolution;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbabilityServiceTest {

    @Mock
    private UniversityMapper universityMapper;

    @Mock
    private AdmissionCutoffMapper admissionCutoffMapper;

    @Mock
    private MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;

    @Mock
    private ScoreRankMappingService scoreRankMappingService;

    private RecommendationPolicyService recommendationPolicyService;

    @InjectMocks
    private ProbabilityService probabilityService;

    @BeforeEach
    void setUp() {
        recommendationPolicyService = new RecommendationPolicyService(new RecommendationScoringProperties());
        probabilityService = new ProbabilityService(
                universityMapper,
                admissionCutoffMapper,
                majorAdmissionCutoffMapper,
                scoreRankMappingService,
                recommendationPolicyService,
                new RecommendationScoringProperties()
        );
    }

    @Test
    void evaluate_shouldUseMajorCutoffWhenAvailable() {
        ProbabilityRequest request = new ProbabilityRequest();
        request.setScore(620);
        request.setProvince("浙江");
        request.setSubjectType(SubjectType.PHYSICS);
        request.setUniversityId(2L);
        request.setMajorName("计算机科学与技术");

        University university = university(2L, "宁波大学");
        MajorAdmissionCutoff majorCutoff = majorCutoff(2025, 618, 24000);
        when(scoreRankMappingService.resolveRankOrProvided("浙江", "物理", 620, null))
                .thenReturn(new RankResolution(26000, ScoreRankMappingService.SOURCE_EXACT, 2025));
        when(universityMapper.findById(2L)).thenReturn(university);
        when(majorAdmissionCutoffMapper.findLatestByUniversityAndMajor(2L, "浙江", "物理", "计算机科学与技术"))
                .thenReturn(majorCutoff);

        ProbabilityBreakdownResponse response = probabilityService.evaluate(request);

        assertEquals(2L, response.universityId());
        assertEquals("宁波大学", response.universityName());
        assertEquals("计算机科学与技术", response.majorName());
        assertEquals(618, response.cutoffScore());
        assertEquals(24000, response.minRank());
        assertEquals(2025, response.admissionYear());
        assertEquals(26000, response.userRank());
        assertEquals(ScoreRankMappingService.SOURCE_EXACT, response.rankSource());
        assertNotNull(response.probability());
        assertTrue(response.explanation().contains("综合录取概率"));
    }

    @Test
    void evaluate_shouldFallbackToSchoolCutoffWhenMajorMissing() {
        ProbabilityRequest request = new ProbabilityRequest();
        request.setScore(620);
        request.setProvince("浙江");
        request.setSubjectType(SubjectType.PHYSICS);
        request.setUniversityName("宁波大学");
        request.setMajorName("不存在的专业");

        University university = university(2L, "宁波大学");
        AdmissionCutoff cutoff = admissionCutoff(2025, 612, 28000);
        when(scoreRankMappingService.resolveRankOrProvided("浙江", "物理", 620, null))
                .thenReturn(new RankResolution(26000, ScoreRankMappingService.SOURCE_EXACT, 2025));
        when(universityMapper.findByExactName("宁波大学")).thenReturn(university);
        when(majorAdmissionCutoffMapper.findLatestByUniversityAndMajor(2L, "浙江", "物理", "不存在的专业"))
                .thenReturn(null);
        when(admissionCutoffMapper.findLatestByUniversityAndProvinceSubject(2L, "浙江", "物理"))
                .thenReturn(cutoff);

        ProbabilityBreakdownResponse response = probabilityService.evaluate(request);

        assertEquals(612, response.cutoffScore());
        assertEquals(28000, response.minRank());
        assertEquals(2025, response.admissionYear());
        verify(admissionCutoffMapper).findLatestByUniversityAndProvinceSubject(2L, "浙江", "物理");
    }

    @Test
    void evaluateBatch_shouldDeduplicateAndPreferMajorCutoff() {
        ProbabilityBatchRequest request = new ProbabilityBatchRequest();
        request.setScore(620);
        request.setProvince("浙江");
        request.setSubjectType(SubjectType.PHYSICS);
        request.setUniversityIds(List.of(2L));
        request.setUniversityNames(List.of("宁波大学", "杭州电子科技大学"));
        request.setMajorName("计算机科学与技术");

        University nb = university(2L, "宁波大学");
        University hdu = university(3L, "杭州电子科技大学");
        when(scoreRankMappingService.resolveRankOrProvided("浙江", "物理", 620, null))
                .thenReturn(new RankResolution(26000, ScoreRankMappingService.SOURCE_EXACT, 2025));
        when(universityMapper.findByIds(List.of(2L))).thenReturn(List.of(nb));
        when(universityMapper.findByExactName("宁波大学")).thenReturn(nb);
        when(universityMapper.findByExactName("杭州电子科技大学")).thenReturn(hdu);
        when(majorAdmissionCutoffMapper.findLatestByUniversityAndMajor(2L, "浙江", "物理", "计算机科学与技术"))
                .thenReturn(majorCutoff(2025, 618, 24000));
        when(majorAdmissionCutoffMapper.findLatestByUniversityAndMajor(3L, "浙江", "物理", "计算机科学与技术"))
                .thenReturn(null);
        when(admissionCutoffMapper.findLatestByUniversityAndProvinceSubject(3L, "浙江", "物理"))
                .thenReturn(admissionCutoff(2025, 598, 42000));

        List<ProbabilityBreakdownResponse> responses = probabilityService.evaluateBatch(request);

        assertEquals(2, responses.size());
        assertEquals("宁波大学", responses.get(0).universityName());
        assertEquals("计算机科学与技术", responses.get(0).majorName());
        assertEquals(618, responses.get(0).cutoffScore());
        assertEquals("杭州电子科技大学", responses.get(1).universityName());
        assertEquals(null, responses.get(1).majorName());
        assertEquals(598, responses.get(1).cutoffScore());
    }

    private University university(Long id, String name) {
        University university = new University();
        university.setId(id);
        university.setName(name);
        return university;
    }

    private AdmissionCutoff admissionCutoff(Integer year, Integer score, Integer rank) {
        AdmissionCutoff cutoff = new AdmissionCutoff();
        cutoff.setAdmissionYear(year);
        cutoff.setCutoffScore(score);
        cutoff.setMinRank(rank);
        return cutoff;
    }

    private MajorAdmissionCutoff majorCutoff(Integer year, Integer score, Integer rank) {
        MajorAdmissionCutoff cutoff = new MajorAdmissionCutoff();
        cutoff.setAdmissionYear(year);
        cutoff.setCutoffScore(score);
        cutoff.setMinRank(rank);
        return cutoff;
    }
}
