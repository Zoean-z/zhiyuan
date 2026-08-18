package com.zhiyuan.college.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RecommendationMode {
    SCHOOL_FIRST("学校优先"),
    MAJOR_FIRST("专业优先");

    private final String displayName;

    RecommendationMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static RecommendationMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (RecommendationMode mode : RecommendationMode.values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported recommendationMode: " + value);
    }
}
