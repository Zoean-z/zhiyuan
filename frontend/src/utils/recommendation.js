export const SUBJECT_OPTIONS = [
  { value: "PHYSICS", label: "物理" },
  { value: "HISTORY", label: "历史" }
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

export function normalizeItem(item, fallbackStrategy) {
  const strategy = normalizeStrategy(
    pickValue(item, ["strategy", "strategyType", "type"]) || fallbackStrategy
  );
  return {
    universityName: pickValue(item, ["universityName", "schoolName", "name"]) || "未知院校",
    cutoffScore: pickValue(item, ["cutoffScore", "cutoff", "lastYearCutoff"]),
    scoreGap: pickValue(item, ["scoreGap", "gap", "difference"]),
    admissionProbability: pickValue(item, ["admissionProbability", "probability", "chance"]),
    strategy
  };
}

export function dedupeByUniversity(list) {
  const seen = new Set();
  const result = [];
  (list || []).forEach((item) => {
    const name = String(pickValue(item, ["universityName", "schoolName", "name"]) || "")
      .trim()
      .toLowerCase();
    if (!name || !seen.has(name)) {
      if (name) seen.add(name);
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

export function queryTypeTag(type) {
  return type === "score" ? "success" : type === "text" ? "warning" : "info";
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
