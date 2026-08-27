import {
  backendStrategyOfSegKey,
  segKeyOfBackendStrategy,
  strategyOf as strategyOfProbability
} from "./scoreModel.js";

/**
 * 志愿表本地编辑核心。
 *
 * 本模块只负责 45 个志愿位的顺序、分段与本地草稿持久化，不生成院校、专业、
 * 录取线、位次或概率。所有事实型字段必须由后端接口返回后再写入志愿位。
 */

export const TOTAL = 45;
export const SEGMENTS = [
  { key: "rush", label: "冲刺志愿", range: [0, 15], cls: "rush" },
  { key: "safe", label: "稳妥志愿", range: [15, 30], cls: "safe" },
  { key: "guard", label: "保底志愿", range: [30, 45], cls: "guard" }
];

const STORE_KEY = "zhiyuan_volunteer_sheets";
const CURRENT_KEY = "zhiyuan_volunteer_current";

function firstFinite(...values) {
  for (const value of values) {
    const num = Number(value);
    if (value !== null && value !== "" && Number.isFinite(num)) return num;
  }
  return null;
}

/**
 * 统一不同后端响应中的院校字段名，但绝不为缺失院校生成 ID、类型或业务数据。
 */
export function normalizeSchoolLike(schoolLike) {
  if (!schoolLike) return null;
  return {
    id: schoolLike.id ?? schoolLike.schoolId ?? schoolLike.universityId ?? null,
    name: schoolLike.name || schoolLike.universityName || schoolLike.schoolName || "未知院校",
    type: schoolLike.type || schoolLike.schoolType || "",
    province: schoolLike.province || schoolLike.universityProvince || "",
    cutoffScore: firstFinite(schoolLike.cutoffScore, schoolLike.minScore, schoolLike.lowestScore),
    minRank: firstFinite(schoolLike.minRank, schoolLike.lowestRank)
  };
}

/** 后端概率到展示档位的纯映射，不在前端重新计算概率。 */
export function strategyOf(probability) {
  return strategyOfProbability(probability);
}

export { segKeyOfBackendStrategy, backendStrategyOfSegKey };

export function readCurrentSheet() {
  try {
    const raw = JSON.parse(localStorage.getItem(CURRENT_KEY) || "null");
    if (!Array.isArray(raw) || raw.length !== TOTAL) return null;
    return raw;
  } catch {
    return null;
  }
}

export function writeCurrentSheet(slots) {
  try {
    localStorage.setItem(CURRENT_KEY, JSON.stringify(slots));
  } catch {
    /* localStorage 不可用时保留当前页面状态，不生成替代数据 */
  }
}

export function clearCurrentSheet() {
  localStorage.removeItem(CURRENT_KEY);
}

/** 段内第一个空位；段满则落到全局第一个空位。 */
export function appendToCurrentSheet(slot, strategyKey) {
  const segKey = strategyKey === "guarantee" ? "guard" : strategyKey;
  const seg = SEGMENTS.find((item) => item.key === segKey) || SEGMENTS[1];
  const current = readCurrentSheet() || Array.from({ length: TOTAL }, () => null);
  for (let index = seg.range[0]; index < seg.range[1]; index += 1) {
    if (!current[index]) {
      current[index] = slot;
      writeCurrentSheet(current);
      return { ok: true, position: index + 1, segLabel: seg.label };
    }
  }
  for (let index = 0; index < TOTAL; index += 1) {
    if (!current[index]) {
      current[index] = slot;
      writeCurrentSheet(current);
      return { ok: true, position: index + 1, segLabel: "其他空位", fallback: true };
    }
  }
  return { ok: false, message: "志愿表已满，请先清理志愿位" };
}

export function currentSheetCount() {
  const current = readCurrentSheet();
  return current ? current.filter(Boolean).length : 0;
}

/** 志愿位序号到冲稳保分段的固定位置映射。 */
export function segmentOfIndex(index) {
  return SEGMENTS.find((segment) => index >= segment.range[0] && index < segment.range[1]) || SEGMENTS[2];
}

export { STORE_KEY, CURRENT_KEY };
