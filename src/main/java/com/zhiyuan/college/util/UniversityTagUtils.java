package com.zhiyuan.college.util;

import java.util.ArrayList;
import java.util.List;

public final class UniversityTagUtils {

    private UniversityTagUtils() {
    }

    public static Boolean resolveIs985(Boolean is985, String tier) {
        return is985 != null ? is985 : "985".equals(normalizeTier(tier));
    }

    public static Boolean resolveIs211(Boolean is211, String tier) {
        if (is211 != null) {
            return is211;
        }
        String normalizedTier = normalizeTier(tier);
        return "985".equals(normalizedTier) || "211".equals(normalizedTier);
    }

    public static Boolean resolveIsDoubleFirstClass(Boolean isDoubleFirstClass, String tier) {
        if (isDoubleFirstClass != null) {
            return isDoubleFirstClass;
        }
        String normalizedTier = normalizeTier(tier);
        return "985".equals(normalizedTier)
                || "211".equals(normalizedTier)
                || "双一流".equals(normalizedTier);
    }

    public static boolean matchesSchoolLevel(String schoolLevel,
                                             Boolean is985,
                                             Boolean is211,
                                             Boolean isDoubleFirstClass,
                                             String tier) {
        Boolean resolvedIs985 = resolveIs985(is985, tier);
        Boolean resolvedIs211 = resolveIs211(is211, tier);
        Boolean resolvedIsDoubleFirstClass = resolveIsDoubleFirstClass(isDoubleFirstClass, tier);
        return switch (schoolLevel) {
            case "985" -> Boolean.TRUE.equals(resolvedIs985);
            case "211" -> Boolean.TRUE.equals(resolvedIs211);
            case "双一流" -> Boolean.TRUE.equals(resolvedIsDoubleFirstClass);
            case "普通" -> isOrdinarySchool(resolvedIs985, resolvedIs211, resolvedIsDoubleFirstClass);
            default -> false;
        };
    }

    public static boolean isOrdinarySchool(Boolean is985, Boolean is211, Boolean isDoubleFirstClass) {
        return !Boolean.TRUE.equals(is985)
                && !Boolean.TRUE.equals(is211)
                && !Boolean.TRUE.equals(isDoubleFirstClass);
    }

    public static List<String> buildSchoolTags(Boolean is985,
                                               Boolean is211,
                                               Boolean isDoubleFirstClass,
                                               String tier) {
        Boolean resolvedIs985 = resolveIs985(is985, tier);
        Boolean resolvedIs211 = resolveIs211(is211, tier);
        Boolean resolvedIsDoubleFirstClass = resolveIsDoubleFirstClass(isDoubleFirstClass, tier);
        List<String> tags = new ArrayList<>();
        if (Boolean.TRUE.equals(resolvedIs985)) {
            tags.add("985");
        }
        // 双一流 ≡ 211：211 不再单独作为展示标签，按最高标准并入双一流（20260820 概念更新）
        if (Boolean.TRUE.equals(resolvedIsDoubleFirstClass) || Boolean.TRUE.equals(resolvedIs211)) {
            tags.add("双一流");
        }
        return tags;
    }

    private static String normalizeTier(String tier) {
        return tier == null ? "" : tier.trim();
    }
}
