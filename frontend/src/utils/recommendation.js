export const SUBJECT_OPTIONS = [
  { value: "PHYSICS", label: "物理" },
  { value: "HISTORY", label: "历史" }
];

export const RECOMMENDATION_MODE_OPTIONS = [
  { value: "SCHOOL_FIRST", label: "学校优先" },
  { value: "MAJOR_FIRST", label: "专业优先" }
];

export function readStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem("zhiyuan_auth") || "null");
  } catch {
    return null;
  }
}

export function saveStoredAuth(auth) {
  localStorage.setItem("zhiyuan_auth", JSON.stringify(auth));
}

export function clearStoredAuth() {
  localStorage.removeItem("zhiyuan_auth");
}

export function pickValue(obj, keys) {
  for (const key of keys) {
    if (obj && obj[key] !== undefined && obj[key] !== null && obj[key] !== "") {
      return obj[key];
    }
  }
  return null;
}

export function normalizeStrategy(value) {
  const text = String(value || "").toUpperCase();
  if (text.includes("RUSH") || text.includes("冲")) return "rush";
  if (text.includes("SAFE") || text.includes("稳")) return "safe";
  if (text.includes("GUARANTEE") || text.includes("保")) return "guarantee";
  return "safe";
}

function splitTags(value) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item || "").trim()).filter(Boolean);
  }
  if (typeof value === "string") {
    return value
      .split(/[、,，\s]+/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
  return [];
}

function inferTagFlag(explicitValue, tier, schoolTags, expectedTag, tierMatches) {
  if (explicitValue !== null && explicitValue !== undefined) {
    return Boolean(explicitValue);
  }
  if (schoolTags.includes(expectedTag)) {
    return true;
  }
  return tierMatches.includes(String(tier || "").trim());
}

export function normalizeSchoolTags(item) {
  const tier = pickValue(item, ["universityTier", "tier"]);
  const schoolTags = splitTags(pickValue(item, ["schoolTags"]));
  const is985 = inferTagFlag(pickValue(item, ["is985"]), tier, schoolTags, "985", ["985"]);
  const is211 = inferTagFlag(pickValue(item, ["is211"]), tier, schoolTags, "211", ["985", "211"]);
  const isDoubleFirstClass = inferTagFlag(
    pickValue(item, ["isDoubleFirstClass"]),
    tier,
    schoolTags,
    "双一流",
    ["985", "211", "双一流"]
  );

  const normalized = [];
  if (is985) normalized.push("985");
  if (is211) normalized.push("211");
  if (isDoubleFirstClass) normalized.push("双一流");
  schoolTags.forEach((tag) => {
    if (tag !== "普通" && !normalized.includes(tag)) {
      normalized.push(tag);
    }
  });

  return { is985, is211, isDoubleFirstClass, schoolTags: normalized };
}

export function normalizeItem(item, fallbackStrategy) {
  const strategy = normalizeStrategy(
    pickValue(item, ["strategy", "strategyType", "type"]) || fallbackStrategy
  );
  const recommendationMode = pickValue(item, ["recommendationMode"]) || "SCHOOL_FIRST";
  const recommendationBasis = pickValue(item, ["recommendationBasis", "basis"]);
  const userRank = pickValue(item, ["userRank"]);
  const minRank = pickValue(item, ["minRank", "minimumRank"]);
  const rankGap = pickValue(item, ["rankGap"]);
  const schoolTagModel = normalizeSchoolTags(item);
  return {
    recommendationMode,
    universityId: pickValue(item, ["universityId", "schoolId", "id"]),
    universityName: pickValue(item, ["universityName", "schoolName", "name"]) || "未知院校",
    majorName: pickValue(item, ["majorName", "major", "specialtyName"]) || "",
    universityProvince: pickValue(item, ["universityProvince", "province"]),
    universityTier: pickValue(item, ["universityTier", "tier"]),
    is985: schoolTagModel.is985,
    is211: schoolTagModel.is211,
    isDoubleFirstClass: schoolTagModel.isDoubleFirstClass,
    schoolTags: schoolTagModel.schoolTags,
    universityTags: pickValue(item, ["universityTags", "tags"]),
    cutoffScore: pickValue(item, ["cutoffScore", "cutoff", "lastYearCutoff"]),
    scoreGap: pickValue(item, ["scoreGap", "gap", "difference"]),
    userRank,
    minRank,
    rankGap,
    recommendationBasis,
    admissionProbability: pickValue(item, ["admissionProbability", "probability", "chance"]),
    strategy,
    strategyLabel: pickValue(item, ["strategyLabel"]) || strategyLabel(strategy),
    riskScore: pickValue(item, ["riskScore"]),
    matchReasons: Array.isArray(pickValue(item, ["matchReasons"])) ? pickValue(item, ["matchReasons"]) : [],
    explanation: pickValue(item, ["explanation"])
  };
}

export function buildPlanItemKey(item, fallbackStrategy) {
  const model = normalizeItem(item, fallbackStrategy);
  return [
    model.recommendationMode || "SCHOOL_FIRST",
    model.universityId ?? "",
    String(model.universityName || "").trim().toLowerCase(),
    String(model.majorName || "").trim().toLowerCase(),
    model.strategy || normalizeStrategy(fallbackStrategy)
  ].join("::");
}

export function dedupeByUniversity(list) {
  const seen = new Set();
  const result = [];
  (list || []).forEach((item) => {
    const schoolName = String(pickValue(item, ["universityName", "schoolName", "name"]) || "")
      .trim()
      .toLowerCase();
    const majorName = String(pickValue(item, ["majorName", "major", "specialtyName"]) || "")
      .trim()
      .toLowerCase();
    const key = `${schoolName}::${majorName}`;
    if (!schoolName || !seen.has(key)) {
      if (schoolName) seen.add(key);
      result.push(item);
    }
  });
  return result;
}

export function groupByStrategy(items) {
  const buckets = { rush: [], safe: [], guarantee: [] };
  (items || []).forEach((item) => {
    buckets[normalizeStrategy(item?.strategy)].push(item);
  });
  return {
    rush: dedupeByUniversity(buckets.rush),
    safe: dedupeByUniversity(buckets.safe),
    guarantee: dedupeByUniversity(buckets.guarantee)
  };
}

export function buildGroupedFromResult(resultObj) {
  if (Array.isArray(resultObj?.rush) || Array.isArray(resultObj?.safe) || Array.isArray(resultObj?.guarantee)) {
    return {
      rush: Array.isArray(resultObj?.rush) ? resultObj.rush : [],
      safe: Array.isArray(resultObj?.safe) ? resultObj.safe : [],
      guarantee: Array.isArray(resultObj?.guarantee) ? resultObj.guarantee : []
    };
  }
  if (Array.isArray(resultObj?.recommendations)) {
    return groupByStrategy(resultObj.recommendations);
  }
  return { rush: [], safe: [], guarantee: [] };
}

export function queryTypeLabel(type) {
  return type === "score" ? "分数查询" : type === "text" ? "文本查询" : "未知";
}

export function sourceTypeLabel(type) {
  return type === "score" ? "分数查询" : type === "text" ? "文本查询" : "未知";
}

export function queryTypeTag(type) {
  return type === "score" ? "success" : type === "text" ? "warning" : "info";
}

export function sourceTypeTag(type) {
  return type === "score" ? "success" : type === "text" ? "warning" : "info";
}

export function subjectTypeLabel(type) {
  return SUBJECT_OPTIONS.find((item) => item.value === type)?.label || type || "-";
}

export function recommendationModeLabel(mode) {
  return RECOMMENDATION_MODE_OPTIONS.find((item) => item.value === mode)?.label || mode || "学校优先";
}

export function strategyLabel(strategy) {
  const normalized = normalizeStrategy(strategy);
  if (normalized === "rush") return "冲刺";
  if (normalized === "guarantee") return "保底";
  return "稳妥";
}

export function strategyTagType(strategy) {
  const normalized = normalizeStrategy(strategy);
  if (normalized === "rush") return "danger";
  if (normalized === "guarantee") return "success";
  return "warning";
}

export function recommendationBasisLabel(value) {
  if (value === "RANK") return "位次依据";
  if (value === "SCORE") return "分数依据";
  return "综合判断";
}

export function formatDateTime(value) {
  if (!value) return "-";
  const normalized = String(value).replace(" ", "T");
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return date.toLocaleString("zh-CN", { hour12: false });
}
