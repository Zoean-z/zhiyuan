package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiExplanationService {

    public String buildSummary(RecommendationRequest request, int totalCount, Integer userRank, boolean rankBased) {
        if (rankBased && userRank != null) {
            return String.format(
                    "根据%s%s类考生分数%d对应位次%d，系统按院校最低录取位次筛选出%d所院校，建议结合专业偏好和近年位次波动综合判断。",
                    request.getProvince(),
                    request.getSubjectType().getDisplayName(),
                    request.getScore(),
                    userRank,
                    totalCount
            );
        }
        return String.format(
                "根据%s%s类考生分数%d，当前缺少可用位次映射或院校录取位次数据，暂时无法生成位次推荐结果。",
                request.getProvince(),
                request.getSubjectType().getDisplayName(),
                request.getScore(),
                totalCount
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
        return "当前推荐结果缺少完整位次信息。";
    }

    public void enrichItems(RecommendationRequest request, List<RecommendationItemResponse> items) {
        for (int i = 0; i < items.size(); i++) {
            RecommendationItemResponse item = items.get(i);
            item.setExplanation(buildItemExplanation(request, item));
            items.set(i, item);
        }
    }
}
