package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.enums.RecommendationMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiExplanationService {

    public String buildSummary(RecommendationRequest request, int totalCount, Integer userRank, boolean rankBased) {
        RecommendationMode mode = request.getRecommendationMode() == null
                ? RecommendationMode.SCHOOL_FIRST
                : request.getRecommendationMode();
        String targetText = mode == RecommendationMode.MAJOR_FIRST ? "个学校专业组合" : "所院校";
        if (totalCount == 0) {
            return mode == RecommendationMode.MAJOR_FIRST
                    ? String.format("根据%s%s类考生分数%d与专业关键词“%s”，当前暂无可推荐的学校专业结果。",
                    request.getProvince(),
                    request.getSubjectType().getDisplayName(),
                    request.getScore(),
                    request.getMajorKeyword() == null ? "-" : request.getMajorKeyword())
                    : String.format("根据%s%s类考生分数%d，当前暂无可推荐院校结果。",
                    request.getProvince(),
                    request.getSubjectType().getDisplayName(),
                    request.getScore());
        }
        if (rankBased && userRank != null) {
            return String.format(
                    "根据%s%s类考生分数%d对应位次%d，系统按最低录取位次筛选出%d%s，建议结合近年波动综合判断。",
                    request.getProvince(),
                    request.getSubjectType().getDisplayName(),
                    request.getScore(),
                    userRank,
                    totalCount,
                    targetText
            );
        }
        return String.format(
                "根据%s%s类考生分数%d，系统已按最低录取分筛选出%d%s，可作为位次缺失时的兜底参考。",
                request.getProvince(),
                request.getSubjectType().getDisplayName(),
                request.getScore(),
                totalCount,
                targetText
        );
    }

    public String buildItemExplanation(RecommendationRequest request,
                                       RecommendationItemResponse itemResponse) {
        if ("RANK".equals(itemResponse.getRecommendationBasis())
                && itemResponse.getUserRank() != null
                && itemResponse.getMinRank() != null
                && itemResponse.getRankGap() != null) {
            return String.format(
                    "该校近年最低录取位次约为%d，你当前分数%d对应位次为%d，位次差为%d，按位次判断属于%s档。",
                    itemResponse.getMinRank(),
                    request.getScore(),
                    itemResponse.getUserRank(),
                    itemResponse.getRankGap(),
                    itemResponse.getStrategy()
            );
        }
        if ("SCORE".equals(itemResponse.getRecommendationBasis())
                && itemResponse.getCutoffScore() != null
                && itemResponse.getScoreGap() != null) {
            return String.format(
                    "该%s近年最低录取分约为%d，你当前分数%d，分差为%d，按分数判断属于%s档。",
                    itemResponse.getMajorName() == null || itemResponse.getMajorName().isBlank() ? "院校" : "专业",
                    itemResponse.getCutoffScore(),
                    request.getScore(),
                    itemResponse.getScoreGap(),
                    itemResponse.getStrategy()
            );
        }
        return "当前推荐结果缺少完整位次或分数信息。";
    }

    public void enrichItems(RecommendationRequest request, List<RecommendationItemResponse> items) {
        for (int i = 0; i < items.size(); i++) {
            RecommendationItemResponse item = items.get(i);
            item.setExplanation(buildItemExplanation(request, item));
            items.set(i, item);
        }
    }
}
