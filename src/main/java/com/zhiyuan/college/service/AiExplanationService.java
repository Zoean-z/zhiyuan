package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiExplanationService {

    public String buildSummary(RecommendationRequest request, int totalCount) {
        return String.format(
                "根据%s%s类考生分数%d，系统共筛选%d所院校，建议结合专业偏好与城市意向做最终志愿排序。",
                request.getProvince(),
                request.getSubjectType().getDisplayName(),
                request.getScore(),
                totalCount
        );
    }

    public String buildItemExplanation(RecommendationRequest request,
                                       RecommendationItemResponse itemResponse) {
        return String.format(
                "该校近年分数线与当前分差为%d分，属于%s档，建议结合专业录取位次进一步确认。",
                itemResponse.getScoreGap(),
                itemResponse.getStrategy()
        );
    }

    public void enrichItems(RecommendationRequest request, List<RecommendationItemResponse> items) {
        for (int i = 0; i < items.size(); i++) {
            RecommendationItemResponse item = items.get(i);
            items.set(i, new RecommendationItemResponse(
                    item.getUniversityName(),
                    item.getCutoffScore(),
                    item.getScoreGap(),
                    item.getAdmissionProbability(),
                    item.getStrategy(),
                    buildItemExplanation(request, item)
            ));
        }
    }
}
