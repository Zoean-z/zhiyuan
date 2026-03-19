package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.entity.ScoreRankMapping;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ScoreRankMappingMapper extends BaseMapper<ScoreRankMapping> {

    @Select("""
            SELECT rank_value
            FROM score_rank_mapping
            WHERE province = #{province}
              AND subject_type = #{subjectType}
              AND score = #{score}
              AND mapping_year = (
                SELECT MAX(m2.mapping_year)
                FROM score_rank_mapping m2
                WHERE m2.province = #{province}
                  AND m2.subject_type = #{subjectType}
              )
            """)
    Integer findLatestRankValueByProvinceSubjectAndScore(@Param("province") String province,
                                                         @Param("subjectType") String subjectType,
                                                         @Param("score") Integer score);
}
