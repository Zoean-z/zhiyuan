package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorMapper;
import com.zhiyuan.college.model.dto.MajorItemResponse;
import com.zhiyuan.college.model.dto.MajorListResponse;
import com.zhiyuan.college.model.dto.MajorSchoolItemResponse;
import com.zhiyuan.college.model.dto.ProbabilityBreakdownResponse;
import com.zhiyuan.college.model.entity.Major;
import com.zhiyuan.college.service.ScoreRankMappingService.RankResolution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 公开专业目录查询：专业列表（含开设院校数）+ 某专业的开设院校。
 * 数据来自 major 表 + major_admission_cutoff 聚合（覆盖全部 1137 所大学）。
 */
@Service
public class MajorQueryService {

    private final MajorMapper majorMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final ProbabilityService probabilityService;

    public MajorQueryService(MajorMapper majorMapper,
                             MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                             ProbabilityService probabilityService) {
        this.majorMapper = majorMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.probabilityService = probabilityService;
    }

    /** 专业目录：32 个热门专业 + 各专业开设院校数。 */
    public MajorListResponse listMajors() {
        List<Major> majors = majorMapper.findAllOrdered();
        Map<String, Integer> openCounts = new HashMap<>();
        for (Map<String, Object> row : majorAdmissionCutoffMapper.countOpenSchoolsByMajor()) {
            Object name = row.get("majorName");
            Object count = row.get("openSchoolCount");
            if (name != null && count != null) {
                openCounts.put(String.valueOf(name), ((Number) count).intValue());
            }
        }
        List<MajorItemResponse> items = new ArrayList<>();
        for (Major m : majors) {
            Integer count = openCounts.getOrDefault(m.getName(), 0);
            items.add(new MajorItemResponse(
                    m.getId(),
                    m.getName(),
                    m.getCategory(),
                    m.getDegreeType(),
                    m.getSubjectRequirement(),
                    m.getDescription(),
                    count
            ));
        }
        return new MajorListResponse(items, items.size());
    }

    /** 某专业的开设院校列表（最新年份录取线 + 计划数 + 录取概率）。 */
    public List<MajorSchoolItemResponse> schoolsOfMajor(Long majorId,
                                                        String province,
                                                        String subjectType,
                                                        Integer score,
                                                        Integer providedRank) {
        Major major = majorMapper.findByIdCompat(majorId);
        if (major == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "专业不存在: " + majorId);
        }
        List<MajorSchoolItemResponse> schools = majorAdmissionCutoffMapper.findSchoolsByMajorName(
                major.getName(),
                province,
                subjectType
        );
        // 传入分数/位次时计算录取概率（与查大学/详情同一套 ProbabilityService 规则）
        if (score != null || (providedRank != null && providedRank > 0)) {
            RankResolution rank = probabilityService.resolveRank(province, subjectType, score, providedRank);
            for (MajorSchoolItemResponse school : schools) {
                ProbabilityBreakdownResponse probability = probabilityService.buildBreakdown(
                        school.getUniversityId(),
                        school.getUniversityName(),
                        major.getName(),
                        province,
                        subjectType,
                        school.getAdmissionYear(),
                        score,
                        rank,
                        school.getCutoffScore(),
                        school.getMinRank()
                );
                school.setProbability(probability);
            }
        }
        return schools;
    }
}
