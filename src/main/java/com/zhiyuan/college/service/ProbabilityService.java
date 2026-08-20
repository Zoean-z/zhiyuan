package com.zhiyuan.college.service;

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
import com.zhiyuan.college.model.enums.StrategyType;
import com.zhiyuan.college.service.RecommendationPolicyService.ProbabilityBreakdown;
import com.zhiyuan.college.service.ScoreRankMappingService.RankResolution;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 录取概率接口的服务层。前端不再自己复刻概率公式，统一调这里。
 */
@Service
public class ProbabilityService {

    private final UniversityMapper universityMapper;
    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final ScoreRankMappingService scoreRankMappingService;
    private final RecommendationPolicyService recommendationPolicyService;
    private final RecommendationScoringProperties scoringProperties;

    public ProbabilityService(UniversityMapper universityMapper,
                             AdmissionCutoffMapper admissionCutoffMapper,
                             MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                             ScoreRankMappingService scoreRankMappingService,
                             RecommendationPolicyService recommendationPolicyService,
                             RecommendationScoringProperties scoringProperties) {
        this.universityMapper = universityMapper;
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.scoreRankMappingService = scoreRankMappingService;
        this.recommendationPolicyService = recommendationPolicyService;
        this.scoringProperties = scoringProperties;
    }

    public ProbabilityBreakdownResponse evaluate(ProbabilityRequest request) {
        String province = request.getProvince().trim();
        String subjectType = request.getSubjectType().getDbValue();
        RankResolution rank = resolveRank(province, subjectType, request.getScore(), request.getUserRank());

        University university = resolveUniversity(request.getUniversityId(), request.getUniversityName());
        String majorName = trimToNull(request.getMajorName());
        Integer cutoffScore = request.getCutoffScore();
        Integer minRank = request.getMinRank();
        Integer admissionYear = null;

        if (cutoffScore == null && minRank == null && university != null) {
            if (majorName != null) {
                MajorAdmissionCutoff majorCutoff = majorAdmissionCutoffMapper.findLatestByUniversityAndMajor(
                        university.getId(), province, subjectType, majorName);
                if (majorCutoff != null) {
                    cutoffScore = majorCutoff.getCutoffScore();
                    minRank = majorCutoff.getMinRank();
                    admissionYear = majorCutoff.getAdmissionYear();
                }
            }
            if (cutoffScore == null && minRank == null) {
                AdmissionCutoff cutoff = admissionCutoffMapper.findLatestByUniversityAndProvinceSubject(
                        university.getId(), province, subjectType);
                if (cutoff != null) {
                    cutoffScore = cutoff.getCutoffScore();
                    minRank = cutoff.getMinRank();
                    admissionYear = cutoff.getAdmissionYear();
                }
            }
        }

        if (university == null && cutoffScore == null && minRank == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "universityId / universityName / cutoffScore is required");
        }

        return buildBreakdown(
                university == null ? request.getUniversityId() : university.getId(),
                university == null ? trimToNull(request.getUniversityName()) : university.getName(),
                majorName,
                province,
                subjectType,
                admissionYear,
                request.getScore(),
                rank,
                cutoffScore,
                minRank
        );
    }

    public List<ProbabilityBreakdownResponse> evaluateBatch(ProbabilityBatchRequest request) {
        String province = request.getProvince().trim();
        String subjectType = request.getSubjectType().getDbValue();
        String requestedMajorName = trimToNull(request.getMajorName());
        RankResolution rank = resolveRank(province, subjectType, request.getScore(), request.getUserRank());

        List<University> universities = new ArrayList<>();
        if (request.getUniversityIds() != null && !request.getUniversityIds().isEmpty()) {
            List<University> byIds = universityMapper.findByIds(request.getUniversityIds());
            if (byIds != null) {
                universities.addAll(byIds);
            }
        }
        if (request.getUniversityNames() != null) {
            for (String rawName : request.getUniversityNames()) {
                String name = trimToNull(rawName);
                if (name == null) {
                    continue;
                }
                University found = universityMapper.findByExactName(name);
                if (found == null) {
                    continue;
                }
                boolean duplicated = false;
                for (University existing : universities) {
                    if (existing.getId() != null && existing.getId().equals(found.getId())) {
                        duplicated = true;
                        break;
                    }
                }
                if (!duplicated) {
                    universities.add(found);
                }
            }
        }

        List<ProbabilityBreakdownResponse> results = new ArrayList<>();
        for (University university : universities) {
            Integer admissionYear = null;
            Integer cutoffScore = null;
            Integer minRank = null;
            String resolvedMajorName = null;
            if (requestedMajorName != null) {
                MajorAdmissionCutoff majorCutoff = majorAdmissionCutoffMapper.findLatestByUniversityAndMajor(
                        university.getId(), province, subjectType, requestedMajorName);
                if (majorCutoff != null) {
                    admissionYear = majorCutoff.getAdmissionYear();
                    cutoffScore = majorCutoff.getCutoffScore();
                    minRank = majorCutoff.getMinRank();
                    resolvedMajorName = requestedMajorName;
                }
            }
            if (cutoffScore == null && minRank == null) {
                AdmissionCutoff cutoff = admissionCutoffMapper.findLatestByUniversityAndProvinceSubject(
                        university.getId(), province, subjectType);
                if (cutoff != null) {
                    admissionYear = cutoff.getAdmissionYear();
                    cutoffScore = cutoff.getCutoffScore();
                    minRank = cutoff.getMinRank();
                }
            }
            results.add(buildBreakdown(
                    university.getId(),
                    university.getName(),
                    resolvedMajorName,
                    province,
                    subjectType,
                    admissionYear,
                    request.getScore(),
                    rank,
                    cutoffScore,
                    minRank
            ));
        }
        return results;
    }

    public RankResolution resolveRank(String province, String subjectType, Integer score, Integer providedRank) {
        return scoreRankMappingService.resolveRankOrProvided(province, subjectType, score, providedRank);
    }

    public ProbabilityBreakdownResponse buildBreakdown(Long universityId,
                                                      String universityName,
                                                      String majorName,
                                                      String province,
                                                      String subjectType,
                                                      Integer admissionYear,
                                                      Integer userScore,
                                                      RankResolution rank,
                                                      Integer cutoffScore,
                                                      Integer minRank) {
        Integer userRank = rank == null ? null : rank.rank();
        String rankSource = rank == null ? ScoreRankMappingService.SOURCE_NONE : rank.source();
        ProbabilityBreakdown breakdown = recommendationPolicyService.explain(userScore, userRank, cutoffScore, minRank);
        StrategyType strategy = breakdown.strategy();
        return new ProbabilityBreakdownResponse(
                universityId,
                universityName,
                majorName,
                province,
                subjectType,
                admissionYear,
                userScore,
                userRank,
                rankSource,
                cutoffScore,
                minRank,
                breakdown.scoreGap(),
                breakdown.rankGap(),
                breakdown.rankProbability(),
                breakdown.scoreProbability(),
                breakdown.probability(),
                strategy == null ? null : strategy.name(),
                strategyLabel(strategy),
                breakdown.recommended(),
                scoringProperties.getRankWeight(),
                scoringProperties.getScoreWeight(),
                scoringProperties.getMinimumProbability(),
                buildExplanation(province, subjectType, admissionYear, userScore, userRank, rankSource,
                        cutoffScore, minRank, breakdown, strategy)
        );
    }

    public static String strategyLabel(StrategyType strategy) {
        if (strategy == null) {
            return null;
        }
        return switch (strategy) {
            case RUSH -> "冲刺";
            case SAFE -> "稳妥";
            case GUARANTEE -> "保底";
        };
    }

    public static String rankSourceLabel(String rankSource) {
        if (rankSource == null) {
            return "无位次数据";
        }
        return switch (rankSource) {
            case ScoreRankMappingService.SOURCE_EXACT -> "一分一段表精确对应";
            case ScoreRankMappingService.SOURCE_INTERPOLATED -> "一分一段插值估算";
            case ScoreRankMappingService.SOURCE_EXTRAPOLATED -> "一分一段外推估算";
            case ScoreRankMappingService.SOURCE_PROVIDED -> "你手动填写";
            default -> "无位次数据";
        };
    }

    private String buildExplanation(String province,
                                    String subjectType,
                                    Integer admissionYear,
                                    Integer userScore,
                                    Integer userRank,
                                    String rankSource,
                                    Integer cutoffScore,
                                    Integer minRank,
                                    ProbabilityBreakdown breakdown,
                                    StrategyType strategy) {
        StringBuilder text = new StringBuilder();
        if (cutoffScore == null && minRank == null) {
            text.append("暂无 ").append(province).append(subjectType).append(" 的录取数据，无法测算录取概率。");
            return text.toString();
        }

        if (admissionYear != null) {
            text.append(admissionYear).append(" 年");
        }
        text.append(province).append(subjectType).append("参考线：");
        text.append(cutoffScore == null ? "投档分未知" : ("投档分 " + cutoffScore + " 分"));
        text.append("、");
        text.append(minRank == null ? "最低位次未知" : ("最低位次 " + minRank));
        text.append("。");

        if (userScore != null) {
            text.append("你的成绩 ").append(userScore).append(" 分");
            if (userRank != null) {
                text.append("，对应位次约 ").append(userRank).append("（").append(rankSourceLabel(rankSource)).append("）");
            }
            text.append("。");
        }

        boolean hasGap = false;
        if (breakdown.rankGap() != null) {
            text.append("位次差 ").append(breakdown.rankGap()).append(" 名（正数为你更靠前）");
            hasGap = true;
        }
        if (breakdown.scoreGap() != null) {
            text.append(hasGap ? "，" : "").append("分数差 ").append(breakdown.scoreGap()).append(" 分");
            hasGap = true;
        }
        if (hasGap) {
            text.append("。");
        }

        if (breakdown.probability() == null) {
            text.append("差距超出模型可测算区间，视为极低概率。");
            return text.toString();
        }

        if (breakdown.rankProbability() != null && breakdown.scoreProbability() != null) {
            text.append("位次口径 ").append(breakdown.rankProbability()).append("%、分数口径 ")
                    .append(breakdown.scoreProbability()).append("%，按位次 ")
                    .append(percent(scoringProperties.getRankWeight())).append("% + 分数 ")
                    .append(percent(scoringProperties.getScoreWeight())).append("% 加权，");
        } else if (breakdown.rankProbability() != null) {
            text.append("仅位次口径可用，");
        } else {
            text.append("仅分数口径可用，");
        }
        text.append("综合录取概率 ").append(breakdown.probability()).append("%");
        if (strategy != null) {
            text.append("，属于").append(strategyLabel(strategy)).append("志愿");
        }
        text.append("。");
        if (!breakdown.recommended()) {
            text.append("低于推荐门槛 ").append(scoringProperties.getMinimumProbability()).append("%，智能推荐列表不会出现。");
        }
        return text.toString();
    }

    private int percent(double weight) {
        return (int) Math.round(weight * 100);
    }

    private University resolveUniversity(Long universityId, String universityName) {
        if (universityId != null) {
            University university = universityMapper.findById(universityId);
            if (university == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "university not found");
            }
            return university;
        }
        String name = trimToNull(universityName);
        if (name == null) {
            return null;
        }
        return universityMapper.findByExactName(name);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
