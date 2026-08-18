package com.zhiyuan.college.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.zhiyuan.college.model.dto.RankLookupResponse;
import com.zhiyuan.college.model.dto.ScoreRankCurveResponse;
import com.zhiyuan.college.model.dto.ScoreRankPointResponse;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.ScoreRankMappingService;
import com.zhiyuan.college.service.ScoreRankMappingService.RankResolution;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ScoreRankControllerTest {

    @Mock
    private ScoreRankMappingService scoreRankMappingService;

    @InjectMocks
    private ScoreRankController scoreRankController;

    @Test
    void rank_shouldReturnInterpolatedRankSourceLabel() {
        when(scoreRankMappingService.resolveRank("浙江", "物理", 625))
                .thenReturn(new RankResolution(24000, ScoreRankMappingService.SOURCE_INTERPOLATED, 2025));

        RankLookupResponse response = scoreRankController.rank(" 浙江 ", SubjectType.PHYSICS, 625);

        assertEquals("浙江", response.province());
        assertEquals("物理", response.subjectType());
        assertEquals(2025, response.mappingYear());
        assertEquals(625, response.score());
        assertEquals(24000, response.rank());
        assertEquals(ScoreRankMappingService.SOURCE_INTERPOLATED, response.rankSource());
        assertEquals("一分一段插值估算", response.rankSourceLabel());
    }

    @Test
    void curve_shouldReturnPointsFromService() {
        List<ScoreRankPointResponse> points = List.of(
                new ScoreRankPointResponse(610, 31000),
                new ScoreRankPointResponse(620, 26000),
                new ScoreRankPointResponse(630, 22000)
        );
        when(scoreRankMappingService.getLatestCurve("浙江", "历史")).thenReturn(points);
        when(scoreRankMappingService.getLatestMappingYear("浙江", "历史")).thenReturn(2025);

        ScoreRankCurveResponse response = scoreRankController.curve("浙江", SubjectType.HISTORY);

        assertEquals("浙江", response.province());
        assertEquals("历史", response.subjectType());
        assertEquals(2025, response.mappingYear());
        assertEquals(3, response.pointCount());
        assertEquals(points, response.points());
    }

    @Test
    void rank_shouldRejectBlankProvince() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> scoreRankController.rank("   ", SubjectType.PHYSICS, 620)
        );

        assertEquals("400 BAD_REQUEST", ex.getStatusCode().toString());
        assertEquals("province is required", ex.getReason());
    }
}
