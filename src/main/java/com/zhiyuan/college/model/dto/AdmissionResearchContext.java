package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** An explicit read-only research request; historical lines are always loaded server-side. */
public record AdmissionResearchContext(
        @NotNull @Positive Long schoolId,
        @Size(max = 100) String majorName,
        @Size(max = 30) String province,
        @Size(max = 20) String subjectType,
        @Min(100) @Max(750) Integer score,
        @Positive Integer userRank,
        @NotNull @Min(2020) @Max(2100) Integer targetYear,
        @Size(max = 20) String batch,
        @Size(max = 2) List<@Size(max = 20) String> secondSubjects,
        LocalDate asOfDate) {}
