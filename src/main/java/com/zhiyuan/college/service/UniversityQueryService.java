package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.mapper.UniversityMapper;
import com.zhiyuan.college.model.dto.CutoffHistoryItemResponse;
import com.zhiyuan.college.model.dto.ProbabilityBreakdownResponse;
import com.zhiyuan.college.model.dto.UniversityDetailResponse;
import com.zhiyuan.college.model.dto.UniversityFilterOptionsResponse;
import com.zhiyuan.college.model.dto.UniversityListItemResponse;
import com.zhiyuan.college.model.dto.UniversityListResponse;
import com.zhiyuan.college.model.dto.UniversityMajorItemResponse;
import com.zhiyuan.college.model.entity.AdmissionCutoff;
import com.zhiyuan.college.model.entity.MajorAdmissionCutoff;
import com.zhiyuan.college.model.entity.University;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.ScoreRankMappingService.RankResolution;
import com.zhiyuan.college.util.UniversityTagUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 公开院校查询（查大学 / 院校详情）。不需要登录，也不走管理后台接口。
 */
@Service
public class UniversityQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String FALLBACK_PROVINCE = "浙江";

    private final UniversityMapper universityMapper;
    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final ProbabilityService probabilityService;

    public UniversityQueryService(UniversityMapper universityMapper,
                                 AdmissionCutoffMapper admissionCutoffMapper,
                                 MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                                 ProbabilityService probabilityService) {
        this.universityMapper = universityMapper;
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.probabilityService = probabilityService;
    }

    public UniversityListResponse list(String examProvince,
                                      String subjectType,
                                      String schoolProvince,
                                      String level,
                                      String tag,
                                      String keyword,
                                      Integer score,
                                      Integer providedRank,
                                      String sort,
                                      int page,
                                      int size,
                                      boolean withDataOnly) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        String normalizedLevel = trimToNull(level);

        List<University> universities = universityMapper.findForPublicList(
                trimToNull(schoolProvince), trimToNull(keyword), trimToNull(tag));
        if (universities == null) {
            universities = List.of();
        }

        RankResolution rank = probabilityService.resolveRank(examProvince, subjectType, score, providedRank);
        Map<Long, AdmissionCutoff> latestCutoffs = new HashMap<>();
        List<AdmissionCutoff> cutoffs = admissionCutoffMapper.findLatestPerUniversity(examProvince, subjectType);
        if (cutoffs != null) {
            for (AdmissionCutoff cutoff : cutoffs) {
                if (cutoff != null && cutoff.getUniversityId() != null) {
                    latestCutoffs.putIfAbsent(cutoff.getUniversityId(), cutoff);
                }
            }
        }

        List<UniversityListItemResponse> items = new ArrayList<>();
        for (University university : universities) {
            if (normalizedLevel != null && !UniversityTagUtils.matchesSchoolLevel(
                    normalizedLevel,
                    university.getIs985(),
                    university.getIs211(),
                    university.getIsDoubleFirstClass(),
                    university.getTier())) {
                continue;
            }
            AdmissionCutoff cutoff = latestCutoffs.get(university.getId());
            if (withDataOnly && cutoff == null) {
                continue;
            }
            ProbabilityBreakdownResponse probability = !canEvaluate(score, rank) ? null : probabilityService.buildBreakdown(
                    university.getId(),
                    university.getName(),
                    null,
                    examProvince,
                    subjectType,
                    cutoff == null ? null : cutoff.getAdmissionYear(),
                    score,
                    rank,
                    cutoff == null ? null : cutoff.getCutoffScore(),
                    cutoff == null ? null : cutoff.getMinRank()
            );
            items.add(new UniversityListItemResponse(
                    university.getId(),
                    university.getName(),
                    university.getProvince(),
                    university.getTier(),
                    UniversityTagUtils.resolveIs985(university.getIs985(), university.getTier()),
                    UniversityTagUtils.resolveIs211(university.getIs211(), university.getTier()),
                    UniversityTagUtils.resolveIsDoubleFirstClass(university.getIsDoubleFirstClass(), university.getTier()),
                    UniversityTagUtils.buildSchoolTags(
                            university.getIs985(),
                            university.getIs211(),
                            university.getIsDoubleFirstClass(),
                            university.getTier()),
                    university.getTags(),
                    cutoff == null ? null : cutoff.getAdmissionYear(),
                    cutoff == null ? null : cutoff.getCutoffScore(),
                    cutoff == null ? null : cutoff.getMinRank(),
                    probability
            ));
        }

        items.sort(buildComparator(sort));
        int total = items.size();
        long requestedOffset = (long) (safePage - 1) * safeSize;
        int fromIndex = (int) Math.min(requestedOffset, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<UniversityListItemResponse> pageItems = new ArrayList<>(items.subList(fromIndex, toIndex));
        return new UniversityListResponse(
                safePage,
                safeSize,
                total,
                examProvince,
                subjectType,
                rank.rank(),
                rank.source(),
                pageItems
        );
    }

    public UniversityDetailResponse detail(Long universityId,
                                           String examProvince,
                                           String subjectType,
                                           Integer score,
                                           Integer providedRank) {
        University university = universityMapper.findById(universityId);
        if (university == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "university not found");
        }

        RankResolution rank = probabilityService.resolveRank(examProvince, subjectType, score, providedRank);

        List<AdmissionCutoff> history = admissionCutoffMapper.findHistoryByUniversityAndProvinceSubject(
                universityId, examProvince, subjectType);
        if (history == null) {
            history = List.of();
        }
        List<CutoffHistoryItemResponse> historyItems = new ArrayList<>();
        for (int index = 0; index < history.size(); index++) {
            AdmissionCutoff current = history.get(index);
            AdmissionCutoff older = index + 1 < history.size() ? history.get(index + 1) : null;
            Integer scoreDelta = older == null || current.getCutoffScore() == null || older.getCutoffScore() == null
                    ? null : current.getCutoffScore() - older.getCutoffScore();
            Integer rankDelta = older == null || current.getMinRank() == null || older.getMinRank() == null
                    ? null : current.getMinRank() - older.getMinRank();
            historyItems.add(new CutoffHistoryItemResponse(
                    current.getAdmissionYear(),
                    current.getProvince(),
                    current.getSubjectType(),
                    current.getCutoffScore(),
                    current.getMinRank(),
                    scoreDelta,
                    rankDelta
            ));
        }

        AdmissionCutoff latest = history.isEmpty() ? null : history.get(0);
        ProbabilityBreakdownResponse probability = !canEvaluate(score, rank) ? null : probabilityService.buildBreakdown(
                university.getId(),
                university.getName(),
                null,
                examProvince,
                subjectType,
                latest == null ? null : latest.getAdmissionYear(),
                score,
                rank,
                latest == null ? null : latest.getCutoffScore(),
                latest == null ? null : latest.getMinRank()
        );

        List<MajorAdmissionCutoff> majorRows = majorAdmissionCutoffMapper.findAllByUniversityAndProvinceSubject(
                universityId, examProvince, subjectType);
        LinkedHashMap<String, MajorAdmissionCutoff> latestByMajor = new LinkedHashMap<>();
        if (majorRows != null) {
            for (MajorAdmissionCutoff row : majorRows) {
                if (row == null) {
                    continue;
                }
                String majorName = row.getMajorName() == null ? "" : row.getMajorName();
                latestByMajor.putIfAbsent(majorName, row);
            }
        }
        List<UniversityMajorItemResponse> majors = new ArrayList<>();
        for (MajorAdmissionCutoff row : latestByMajor.values()) {
            ProbabilityBreakdownResponse majorProbability = !canEvaluate(score, rank) ? null : probabilityService.buildBreakdown(
                    university.getId(),
                    university.getName(),
                    row.getMajorName(),
                    examProvince,
                    subjectType,
                    row.getAdmissionYear(),
                    score,
                    rank,
                    row.getCutoffScore(),
                    row.getMinRank()
            );
            majors.add(new UniversityMajorItemResponse(
                    row.getMajorName(),
                    row.getAdmissionYear(),
                    row.getCutoffScore(),
                    row.getMinRank(),
                    majorProbability
            ));
        }

        return new UniversityDetailResponse(
                university.getId(),
                university.getName(),
                university.getProvince(),
                university.getTier(),
                UniversityTagUtils.resolveIs985(university.getIs985(), university.getTier()),
                UniversityTagUtils.resolveIs211(university.getIs211(), university.getTier()),
                UniversityTagUtils.resolveIsDoubleFirstClass(university.getIsDoubleFirstClass(), university.getTier()),
                UniversityTagUtils.buildSchoolTags(
                        university.getIs985(),
                        university.getIs211(),
                        university.getIsDoubleFirstClass(),
                        university.getTier()),
                university.getTags(),
                examProvince,
                subjectType,
                rank.rank(),
                rank.source(),
                probability,
                historyItems,
                majors
        );
    }

    public UniversityFilterOptionsResponse filterOptions() {
        List<String> schoolProvinces = safeList(universityMapper.findDistinctProvinces());
        List<String> examProvinces = safeList(admissionCutoffMapper.findDistinctProvinces());
        List<String> subjectTypes = Arrays.stream(SubjectType.values()).map(SubjectType::getDisplayName).toList();
        List<String> levels = List.of("985", "211", "双一流", "普通");

        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String raw : safeList(universityMapper.findDistinctTagValues())) {
            if (raw == null) {
                continue;
            }
            for (String part : raw.split("[,，/、\\s]+")) {
                String tag = part.trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }
        return new UniversityFilterOptionsResponse(
                schoolProvinces,
                examProvinces,
                subjectTypes,
                levels,
                new ArrayList<>(tags)
        );
    }

    /** 前端没传考生省份时，用库里真实存在录取数据的省份兼底。 */
    public String defaultExamProvince() {
        List<String> provinces = admissionCutoffMapper.findDistinctProvinces();
        if (provinces == null || provinces.isEmpty()) {
            return FALLBACK_PROVINCE;
        }
        return provinces.get(0);
    }

    private Comparator<UniversityListItemResponse> buildComparator(String sort) {
        String key = sort == null ? "" : sort.trim();
        Comparator<UniversityListItemResponse> byName = Comparator.comparing(
                UniversityListItemResponse::name, Comparator.nullsLast(String::compareTo));
        Comparator<UniversityListItemResponse> byScoreDesc = Comparator.<UniversityListItemResponse, Integer>comparing(
                UniversityListItemResponse::cutoffScore, Comparator.nullsLast(Comparator.reverseOrder()));
        return switch (key) {
            case "score_asc" -> Comparator.<UniversityListItemResponse, Integer>comparing(
                    UniversityListItemResponse::cutoffScore, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(byName);
            case "rank_asc" -> Comparator.<UniversityListItemResponse, Integer>comparing(
                    UniversityListItemResponse::minRank, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(byName);
            case "name" -> byName;
            case "probability_desc" -> Comparator.<UniversityListItemResponse, Integer>comparing(
                    item -> item.probability() == null ? null : item.probability().probability(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(byScoreDesc)
                    .thenComparing(byName);
            default -> byScoreDesc.thenComparing(byName);
        };
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean canEvaluate(Integer score, RankResolution rank) {
        return score != null || (rank != null && rank.rank() != null);
    }
}
