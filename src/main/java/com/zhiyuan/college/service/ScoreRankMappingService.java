package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.ScoreRankMappingMapper;
import org.springframework.stereotype.Service;

@Service
public class ScoreRankMappingService {

    private final ScoreRankMappingMapper scoreRankMappingMapper;

    public ScoreRankMappingService(ScoreRankMappingMapper scoreRankMappingMapper) {
        this.scoreRankMappingMapper = scoreRankMappingMapper;
    }

    public Integer resolveUserRank(String province, String subjectType, Integer score) {
        if (province == null || province.isBlank() || subjectType == null || score == null) {
            return null;
        }
        return scoreRankMappingMapper.findLatestRankValueByProvinceSubjectAndScore(province, subjectType, score);
    }
}
