package com.zhiyuan.college.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SubjectType {
    PHYSICS("物理"),
    HISTORY("历史");

    private final String displayName;

    SubjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDbValue() {
        return displayName;
    }

    @JsonCreator
    public static SubjectType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (SubjectType subjectType : SubjectType.values()) {
            if (subjectType.name().equalsIgnoreCase(value)
                    || subjectType.displayName.equals(value)) {
                return subjectType;
            }
        }
        throw new IllegalArgumentException("Unsupported subjectType: " + value);
    }
}
