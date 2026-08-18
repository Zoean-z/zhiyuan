package com.zhiyuan.college.config;

import com.zhiyuan.college.model.enums.SubjectType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 让 GET 查询参数同时兼容 PHYSICS/HISTORY 与 物理/历史。
 * Jackson 的 @JsonCreator 只负责 JSON body，Spring MVC 查询参数需要单独的 Converter。
 */
@Component
public class SubjectTypeConverter implements Converter<String, SubjectType> {

    @Override
    public SubjectType convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return SubjectType.fromValue(source.trim());
    }
}
