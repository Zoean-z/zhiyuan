/**
 * planSync.js——前后端志愿数据的双向桥接
 *
 * 【为什么需要这个文件】
 * Word 文档里的批注是「这里用的是旧数据结构 / 旧的和新的只能留一个会冲突」。
 * 项目里确实存在两套志愿数据结构：
 *
 *   A. 新前端（mnzy 风格模拟填报器）：localStorage 里的 45 个志愿位数组
 *      slots[i] = null | { schoolId, schoolName, prob, majorNames: string[], adjust }
 *      位置本身就是涨度：0-14 冲 / 15-29 稳 / 30-44 保
 *
 *   B. 原版后端（UserPlan / plan_item，本次不改后端）：扇形 items 列表
 *      { universityName, majorName, admissionProbability, minRank, strategy: "rush"|"safe"|"guarantee" }
 *
 * 后端不改，所以不能“只留一个”，而是定一个唯一的映射规则：
 *   - 编辑时以 A 为唯一真相（用户在填报器里选校选专业）
 *   - 保存 / 同步 / 历史以 B 为存储（走 /api/plans、/api/plans/current）
 *   - 两边只能通过本文件互转，页面里不再各自拼字段
 */

import {
  SEGMENTS,
  TOTAL,
  normalizeSchoolLike,
  readCurrentSheet,
  segmentOfIndex,
  strategyOf,
  writeCurrentSheet
} from "./volunteerCore.js";

/** 前端段 key → 后端 strategy 枚举 */
export const SEG_TO_BACKEND = { rush: "rush", safe: "safe", guard: "guarantee" };
/** 后端 strategy 枚举 → 前端段 key */
export const BACKEND_TO_SEG = { rush: "rush", safe: "safe", guarantee: "guard" };

export const SEG_LABEL = { rush: "冲击", safe: "稳妥", guard: "保底" };

function emptySlots() {
  return Array.from({ length: TOTAL }, () => null);
}

/** 志愿位 → 一行可读文本 */
export function describeSlot(slot) {
  if (!slot) return "";
  const majors = (slot.majorNames || []).filter(Boolean);
  return majors.length ? `${slot.schoolName}（${majors.join("、")}）` : slot.schoolName || "";
}

/** 本地 45 个志愿位 → 后端 items（一个志愿位一条，专业合并到 majorName） */
export function sheetToPlanItems(slots) {
  const list = Array.isArray(slots) ? slots : readCurrentSheet() || [];
  const items = [];
  list.forEach((slot, index) => {
    if (!slot || !slot.schoolName) return;
    const trustedSchool = slot.schoolSource === "backend" || slot.schoolSource === "plan";
    const seg = segmentOfIndex(index);
    const trustedMajors = slot.majorSource === "backend" || slot.majorSource === "plan";
    const majors = trustedMajors ? (slot.majorNames || []).filter(Boolean) : [];
    const probability = slot.probabilitySource === "backend" && slot.prob != null && Number.isFinite(Number(slot.prob))
      ? Number(slot.prob)
      : null;
    const minRank = slot.dataSource === "backend" && slot.minRank != null && Number.isFinite(Number(slot.minRank))
      ? Number(slot.minRank)
      : null;
    items.push({
      universityName: slot.schoolName,
      universityId: trustedSchool ? slot.schoolId ?? null : null,
      majorName: majors.length ? majors.join("、") : "院校志愿",
      admissionProbability: probability,
      minRank,
      strategy: SEG_TO_BACKEND[seg.key] || "safe",
      volunteerIndex: index + 1,
      adjust: slot.adjust !== false
    });
  });
  return items;
}

/**
 * 后端 items → 本地 45 个志愿位
 * 优先回到 volunteerIndex 原位；否则按 strategy 落到对应段；段满则递补到全局第一个空位
 * （与 volunteerCore.appendToCurrentSheet 的行为保持一致）
 */
export function planItemsToSheet(items) {
  const slots = emptySlots();
  const overflow = [];
  (items || []).forEach((item) => {
    if (!item) return;
    const name = item.universityName || item.schoolName;
    if (!name) return;
    const school = normalizeSchoolLike({ id: item.universityId, name });
    const prob = item.admissionProbability == null ? null : Number(item.admissionProbability);
    const majors = item.majorName && item.majorName !== "院校志愿"
      ? String(item.majorName).split(/[、,，]/).filter(Boolean)
      : [];
    const slot = {
      schoolId: school.id,
      schoolName: name,
      prob,
      minRank: item.minRank ?? null,
      majorNames: majors,
      adjust: item.adjust !== false,
      schoolSource: "plan",
      majorSource: majors.length ? "plan" : null,
      probabilitySource: null,
      dataSource: null
    };
    const fixed = Number(item.volunteerIndex) - 1;
    if (Number.isInteger(fixed) && fixed >= 0 && fixed < TOTAL && !slots[fixed]) {
      slots[fixed] = slot;
      return;
    }
    const segKey = BACKEND_TO_SEG[item.strategy] || (prob == null ? "safe" : strategyOf(prob).key);
    const seg = SEGMENTS.find((s) => s.key === segKey) || SEGMENTS[1];
    let placed = false;
    for (let i = seg.range[0]; i < seg.range[1]; i += 1) {
      if (!slots[i]) {
        slots[i] = slot;
        placed = true;
        break;
      }
    }
    if (!placed) overflow.push(slot);
  });
  overflow.forEach((slot) => {
    for (let i = 0; i < TOTAL; i += 1) {
      if (!slots[i]) {
        slots[i] = slot;
        return;
      }
    }
  });
  return slots;
}

/** 云端方案 → 直接载入本地填报器 */
export function loadPlanItemsIntoSheet(items) {
  const slots = planItemsToSheet(items);
  writeCurrentSheet(slots);
  return slots;
}

/** 本地志愿表统计（已填 / 冲 / 稳 / 保） */
export function sheetStats(slots) {
  const list = Array.isArray(slots) ? slots : readCurrentSheet() || [];
  const stats = { total: TOTAL, filled: 0, rush: 0, safe: 0, guard: 0 };
  list.forEach((slot, index) => {
    if (!slot || !slot.schoolName) return;
    stats.filled += 1;
    stats[segmentOfIndex(index).key] += 1;
  });
  return stats;
}

/** 本地志愿表 → 按涨度分组的展示结构 */
export function sheetGroups(slots) {
  const list = Array.isArray(slots) ? slots : readCurrentSheet() || [];
  return SEGMENTS.map((seg) => ({
    key: seg.key,
    label: SEG_LABEL[seg.key] || seg.label,
    items: list
      .slice(seg.range[0], seg.range[1])
      .map((slot, i) => (slot ? { ...slot, position: seg.range[0] + i + 1 } : null))
      .filter(Boolean)
  }));
}

/** 后端 resultJson → items（兼容旧方案的三种存法） */
export function itemsFromResultJson(resultJson) {
  let parsed = resultJson;
  if (typeof resultJson === "string") {
    try {
      parsed = JSON.parse(resultJson);
    } catch {
      return [];
    }
  }
  if (!parsed) return [];
  if (Array.isArray(parsed)) return parsed;
  if (Array.isArray(parsed.items)) return parsed.items;
  const merged = [];
  ["rush", "safe", "guarantee"].forEach((key) => {
    const list = parsed[key] || parsed[`${key}Items`];
    if (Array.isArray(list)) list.forEach((item) => merged.push({ ...item, strategy: key }));
  });
  return merged;
}
