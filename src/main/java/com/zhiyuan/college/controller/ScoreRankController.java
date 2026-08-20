package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.RankLookupResponse;
import com.zhiyuan.college.model.dto.ScoreRankCurveResponse;
import com.zhiyuan.college.model.dto.ScoreRankPointResponse;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.ProbabilityService;
import com.zhiyuan.college.service.ScoreRankMappingService;
import com.zhiyuan.college.service.ScoreRankMappingService.RankResolution;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 一分一段（分数 <-> 位次）公开接口。/api/meta/** 已经是免登录白名单。
 */
@RestController
@RequestMapping("/api/meta")
@Validated
public class ScoreRankController {

    private final ScoreRankMappingService scoreRankMappingService;

    public ScoreRankController(ScoreRankMappingService scoreRankMappingService) {
        this.scoreRankMappingService = scoreRankMappingService;
    }

    @GetMapping("/score-rank")
    public ScoreRankCurveResponse curve(@RequestParam("province") @Size(max = 20) String province,
                                       @RequestParam("subjectType") SubjectType subjectType) {
        validate(province, subjectType);
        String normalizedProvince = province.trim();
        String dbSubjectType = subjectType.getDbValue();
        List<ScoreRankPointResponse> points = scoreRankMappingService.getLatestCurve(normalizedProvince, dbSubjectType);
        return new ScoreRankCurveResponse(
                normalizedProvince,
                dbSubjectType,
                scoreRankMappingService.getLatestMappingYear(normalizedProvince, dbSubjectType),
                points.size(),
                points
        );
    }

    @GetMapping("/rank")
    public RankLookupResponse rank(@RequestParam("province") @Size(max = 20) String province,
                                   @RequestParam("subjectType") SubjectType subjectType,
                                   @RequestParam("score") @Min(0) @Max(750) Integer score) {
        validate(province, subjectType);
        if (score == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score is required");
        }
        String normalizedProvince = province.trim();
        String dbSubjectType = subjectType.getDbValue();
        RankResolution resolution = scoreRankMappingService.resolveRank(normalizedProvince, dbSubjectType, score);
        return new RankLookupResponse(
                normalizedProvince,
                dbSubjectType,
                resolution.mappingYear(),
                score,
                resolution.rank(),
                resolution.source(),
                ProbabilityService.rankSourceLabel(resolution.source())
        );
    }

    private void validate(String province, SubjectType subjectType) {
        if (province == null || province.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "province is required");
        }
        if (subjectType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subjectType is required");
        }
    }
}
