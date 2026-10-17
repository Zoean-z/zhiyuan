package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorMapper;
import com.zhiyuan.college.mapper.UniversityMapper;
import com.zhiyuan.college.model.dto.CutoffHistoryItemResponse;
import com.zhiyuan.college.model.dto.MajorSchoolItemResponse;
import com.zhiyuan.college.model.dto.ProbabilityBreakdownResponse;
import com.zhiyuan.college.model.dto.UniversityDetailResponse;
import com.zhiyuan.college.model.dto.UniversityFilterOptionsResponse;
import com.zhiyuan.college.model.dto.UniversityListItemResponse;
import com.zhiyuan.college.model.dto.UniversityListResponse;
import com.zhiyuan.college.model.dto.UniversityMajorItemResponse;
import com.zhiyuan.college.model.dto.UniversityRankingItemResponse;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 公开院校查询（查大学 / 院校详情）。不需要登录，也不走管理后台接口。
 */
@Service
public class UniversityQueryService {

    private static final int MAX_PAGE_SIZE = 1000;
    /** 查大学列表重计算结果缓存：TTL 与容量上限（超限整体清空）。 */
    private static final long LIST_CACHE_TTL_MILLIS = 3 * 60 * 1000L;
    private static final int LIST_CACHE_MAX_ENTRIES = 64;
    private final Map<String, CachedList> cachedLists = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, CachedContext> cachedContexts = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class CachedList {
        private volatile List<UniversityListItemResponse> items;
        private volatile long atMillis;
    }

    /** 一次列表计算的公共重 SQL 产物：校线映射 + 计划聚合（仅依赖省份+科类）。 */
    private static final class CachedContext {
        private volatile Map<Long, AdmissionCutoff> latestCutoffs = Map.of();
        private volatile Map<Long, long[]> planAgg = Map.of();
        private volatile long atMillis;
    }

    private CachedContext cachedContexts(String examProvince, String subjectType) {
        String key = examProvince + "|" + subjectType;
        long now = System.currentTimeMillis();
        cachedContexts.values().removeIf(entry -> now - entry.atMillis > LIST_CACHE_TTL_MILLIS);
        if (cachedContexts.size() > 32) {
            cachedContexts.clear();
        }
        CachedContext cached = cachedContexts.computeIfAbsent(key, key0 -> {
            CachedContext context = new CachedContext();
            Map<Long, AdmissionCutoff> cutoffMap = new HashMap<>();
            List<AdmissionCutoff> cutoffs = admissionCutoffMapper.findLatestPerUniversity(examProvince, subjectType);
            if (cutoffs != null) {
                for (AdmissionCutoff cutoff : cutoffs) {
                    if (cutoff != null && cutoff.getUniversityId() != null) {
                        cutoffMap.putIfAbsent(cutoff.getUniversityId(), cutoff);
                    }
                }
            }
            context.latestCutoffs = cutoffMap;
            Map<Long, long[]> planAgg = new HashMap<>();
            if (examProvince != null && !examProvince.isBlank()) {
                List<Map<String, Object>> rows = majorAdmissionCutoffMapper.aggregatePlanByUniversity(examProvince);
                if (rows != null) {
                    for (Map<String, Object> row : rows) {
                        Object uid = row.get("universityId");
                        if (uid == null) {
                            continue;
                        }
                        long[] agg = new long[2];
                        Object plan = row.get("planCount");
                        Object majorsCount = row.get("majorCount");
                        agg[0] = plan == null ? 0 : ((Number) plan).longValue();
                        agg[1] = majorsCount == null ? 0 : ((Number) majorsCount).longValue();
                        planAgg.put(((Number) uid).longValue(), agg);
                    }
                }
            }
            context.planAgg = planAgg;
            context.atMillis = now;
            return context;
        });
        CachedContext snapshot = new CachedContext();
        snapshot.latestCutoffs = cached.latestCutoffs;
        snapshot.planAgg = cached.planAgg;
        return snapshot;
    }
    private static final String FALLBACK_PROVINCE = "浙江";

    private final UniversityMapper universityMapper;
    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final MajorMapper majorMapper;
    private final ProbabilityService probabilityService;

    /**
     * 院校排行：软科中国大学排名（soft_ranking，可溯源），支持院校类型过滤。
     * 与"本地生成榜单"的旧设计不同，排名数据来自软科官方榜单入库（W6 属性补全）。
     */
    public List<UniversityRankingItemResponse> ranking(String schoolType) {
        List<Map<String, Object>> rows = universityMapper.findRanking(trimToNull(schoolType));
        List<UniversityRankingItemResponse> items = new ArrayList<>();
        if (rows == null) {
            return items;
        }
        for (Map<String, Object> row : rows) {
            Boolean is985 = intFlag(row.get("is985"));
            Boolean is211 = intFlag(row.get("is211"));
            Boolean isDoubleFirstClass = intFlag(row.get("isDoubleFirstClass"));
            String tier = stringOrNull(row.get("tier"));
            String tags = stringOrNull(row.get("tags"));
            items.add(new UniversityRankingItemResponse(
                    longOrNull(row.get("id")),
                    stringOrNull(row.get("name")),
                    stringOrNull(row.get("province")),
                    tier,
                    stringOrNull(row.get("nature")),
                    stringOrNull(row.get("schoolType")),
                    is985,
                    is211,
                    isDoubleFirstClass,
                    UniversityTagUtils.buildSchoolTags(is985, is211, isDoubleFirstClass, tier, tags),
                    tags,
                    intOrNull(row.get("softRanking"))
            ));
        }
        return items;
    }

    private Boolean intFlag(Object value) {
        // tinyint(1) 被 JDBC 映射为 Boolean，其他整型列为 Number
        if (value instanceof Boolean b) {
            return b;
        }
        return value instanceof Number number && number.intValue() != 0;
    }

    private String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Long longOrNull(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer intOrNull(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    public UniversityQueryService(UniversityMapper universityMapper,
                                  AdmissionCutoffMapper admissionCutoffMapper,
                                  MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                                  MajorMapper majorMapper,
                                  ProbabilityService probabilityService) {
        this.universityMapper = universityMapper;
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.majorMapper = majorMapper;
        this.probabilityService = probabilityService;
    }

    public UniversityListResponse list(String examProvince,
                                      String subjectType,
                                      String schoolProvince,
                                      String level,
                                      String admissionBatch,
                                      String tag,
                                      String keyword,
                                      Integer score,
                                      Integer providedRank,
                                      String sort,
                                      int page,
                                      int size,
                                      boolean withDataOnly,
                                      String nature,
                                      String schoolType,
                                      Long majorId) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        String normalizedLevel = trimToNull(level);
        String normalizedAdmissionBatch = normalizeAdmissionBatch(admissionBatch);

        List<University> universities = universityMapper.findForPublicList(
                trimToNull(schoolProvince), trimToNull(keyword), trimToNull(tag));
        if (universities == null) {
            universities = List.of();
        }

        // 按专业筛选：先查该专业开设的大学 id 集合（major_admission_cutoff 真实映射）
        java.util.Set<Long> majorSchoolIds = null;
        if (majorId != null) {
            var majorRows = majorAdmissionCutoffMapper.findSchoolsByMajorName(
                    resolveMajorName(majorId), null, null);
            // L-20260921-09：专业筛选解析为空时必须用空集合（返回 0 结果），
            // 不得保持 null 静默退化为"不过滤"——那是 L-08 同族的丢过滤回退
            majorSchoolIds = new java.util.HashSet<>();
            if (majorRows != null) {
                for (MajorSchoolItemResponse row : majorRows) {
                    if (row.getUniversityId() != null) {
                        majorSchoolIds.add(row.getUniversityId());
                    }
                }
            }
        }

        RankResolution rank = probabilityService.resolveRank(examProvince, subjectType, score, providedRank);

        // 重 SQL 上下文缓存（仅依赖省份+科类，与用户分数无关）：校线映射 + 计划聚合。
        // 16 万行级 GROUP BY 每请求重跑是列表慢的主因；TTL 3 分钟，数据导入后自动过期。
        CachedContext context = cachedContexts(examProvince, subjectType);
        Map<Long, AdmissionCutoff> latestCutoffs = new HashMap<>(context.latestCutoffs);
        Map<Long, long[]> planAgg = new HashMap<>(context.planAgg);

        List<UniversityListItemResponse> items = new ArrayList<>();
        for (University university : universities) {
            if (normalizedAdmissionBatch != null && !matchesAdmissionBatch(normalizedAdmissionBatch, university.getTier())) {
                continue;
            }
            if (normalizedLevel != null && !UniversityTagUtils.matchesSchoolLevel(
                    normalizedLevel,
                    university.getIs985(),
                    university.getIs211(),
                    university.getIsDoubleFirstClass(),
                    university.getTier())) {
                continue;
            }
            String natureValue = trimToNull(university.getNature());
            String schoolTypeValue = trimToNull(university.getSchoolType());
            if (trimToNull(nature) != null && !Objects.equals(natureValue, trimToNull(nature))) {
                continue;
            }
            if (trimToNull(schoolType) != null && !Objects.equals(schoolTypeValue, trimToNull(schoolType))) {
                continue;
            }
            if (majorSchoolIds != null && !majorSchoolIds.contains(university.getId())) {
                continue;
            }
            AdmissionCutoff cutoff = latestCutoffs.get(university.getId());
            long[] agg = planAgg.get(university.getId());
            Integer planCount = agg == null ? null : (int) agg[0];
            Integer majorCount = agg == null ? null : (int) agg[1];
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
                    university.getNature(),
                    university.getSchoolType(),
                    UniversityTagUtils.resolveIs985(university.getIs985(), university.getTier()),
                    UniversityTagUtils.resolveIs211(university.getIs211(), university.getTier()),
                    UniversityTagUtils.resolveIsDoubleFirstClass(university.getIsDoubleFirstClass(), university.getTier()),
                    UniversityTagUtils.buildSchoolTags(
                            university.getIs985(),
                            university.getIs211(),
                            university.getIsDoubleFirstClass(),
                            university.getTier(),
                            university.getTags()),
                    university.getTags(),
                    cutoff == null ? null : cutoff.getAdmissionYear(),
                    cutoff == null ? null : cutoff.getCutoffScore(),
                    cutoff == null ? null : cutoff.getMinRank(),
                    cutoff == null ? null : cutoff.getDataKind(),
                    cutoff == null ? null : cutoff.getCalibrationSource(),
                    cutoff == null ? null : cutoff.getSimulationRule(),
                    planCount,
                    majorCount,
                    probability
            ));
        }

        items.sort(buildComparator(sort));

        // 计算结果缓存：排序后的全量 items 与分页无关，重复访问跳过 SQL 和概率计算（列表组装仍需执行）。
        // TTL 短（3 分钟），数据导入后自动过期；容量超限整体清空（防膨胀）。
        String cacheKey = String.join("|",
                String.valueOf(examProvince), String.valueOf(subjectType), String.valueOf(schoolProvince),
                String.valueOf(normalizedLevel), String.valueOf(normalizedAdmissionBatch), String.valueOf(trimToNull(tag)),
                String.valueOf(trimToNull(keyword)), String.valueOf(score), String.valueOf(providedRank),
                String.valueOf(trimToNull(sort)), String.valueOf(withDataOnly),
                String.valueOf(trimToNull(nature)), String.valueOf(trimToNull(schoolType)), String.valueOf(majorId));
        long now = System.currentTimeMillis();
        cachedLists.values().removeIf(entry -> now - entry.atMillis > LIST_CACHE_TTL_MILLIS);
        if (cachedLists.size() > 64) {
            cachedLists.clear();
        }
        List<UniversityListItemResponse> cachedItems = cachedLists.computeIfAbsent(cacheKey, key -> {
            CachedList cached = new CachedList();
            cached.items = items;
            cached.atMillis = now;
            return cached;
        }).items;

        int total = cachedItems.size();
        long requestedOffset = (long) (safePage - 1) * safeSize;
        int fromIndex = (int) Math.min(requestedOffset, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<UniversityListItemResponse> pageItems = new ArrayList<>(cachedItems.subList(fromIndex, toIndex));
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
                    current.getDataKind(),
                    current.getCalibrationSource(),
                    current.getSimulationRule(),
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
        int totalPlanCount = 0;
        for (MajorAdmissionCutoff row : latestByMajor.values()) {
            if (row.getPlanCount() != null) {
                totalPlanCount += row.getPlanCount();
            }
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
                    row.getPlanCount(),
                    row.getDurationYears(),
                    row.getTuitionPerYear(),
                    row.getDataKind(),
                    row.getCalibrationSource(),
                    row.getSimulationRule(),
                    majorProbability
            ));
        }

        return new UniversityDetailResponse(
                university.getId(),
                university.getName(),
                university.getProvince(),
                university.getTier(),
                university.getNature(),
                university.getSchoolType(),
                university.getSoftRanking(),
                university.getPostgraduateRate() == null ? null : university.getPostgraduateRate().toPlainString(),
                university.getHasGraduateSchool(),
                university.getHasDoctorProgram(),
                totalPlanCount,
                latestByMajor.size(),
                UniversityTagUtils.resolveIs985(university.getIs985(), university.getTier()),
                UniversityTagUtils.resolveIs211(university.getIs211(), university.getTier()),
                UniversityTagUtils.resolveIsDoubleFirstClass(university.getIsDoubleFirstClass(), university.getTier()),
                UniversityTagUtils.buildSchoolTags(
                        university.getIs985(),
                        university.getIs211(),
                        university.getIsDoubleFirstClass(),
                        university.getTier(),
                        university.getTags()),
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
        // 双一流 ≡ 211：筛选项不再单列 211（20260820 概念归并）
        List<String> levels = List.of("985", "双一流", "普通");
        List<String> natures = safeList(universityMapper.findDistinctNatures());
        List<String> types = safeList(universityMapper.findDistinctSchoolTypes());

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
                new ArrayList<>(tags),
                natures,
                types
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

    /** 批次维度（本科批/专科批）：与 level 的名校标签语义平行的独立过滤，按 university.tier 前缀归一化；无法识别的取值视为不过滤。 */
    private String normalizeAdmissionBatch(String admissionBatch) {
        String value = trimToNull(admissionBatch);
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "本科", "本科批", "本科批次" -> "本科";
            case "专科", "专科批", "专科批次" -> "专科";
            default -> null;
        };
    }

    private boolean matchesAdmissionBatch(String normalizedBatch, String tier) {
        if (normalizedBatch == null || tier == null || tier.isBlank()) {
            return false;
        }
        return tier.trim().startsWith(normalizedBatch);
    }

    /** majorId → major.name（查专业筛选用的大学集合）。 */
    private String resolveMajorName(Long majorId) {
        var major = majorMapper.findByIdCompat(majorId);
        return major == null ? null : major.getName();
    }
}
