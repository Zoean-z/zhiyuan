/**
 * ══════════════════════════════════════════════════════════════
 *  考生信息（高考信息表单）全站唯一数据源
 * ══════════════════════════════════════════════════════════════
 *
 * 解决 word 文档里的问题：「这里上一页刚填的分数，根本没用上」。
 *
 * 改造前：
 *   - 首页「模拟报志愿」面板的分数/选科  → 只存在 HomeView 的局部 ref，跳转即丢
 *   - 志愿填报页「高考信息表单」的分数   → 只存在 VolunteerView 的局部 ref
 *   - 智能选大学页的分数                → 又是一份局部 ref
 *   - 登录用户的 score/subjectType      → 只在 /recommend 里用
 * 四份数据互不相通，所以「上一页填的分数」在下一页当然用不上。
 *
 * 改造后：任何页面读写考生信息都必须经过这里（reactive 单例 + localStorage 持久化），
 * 填一次，全站（首页测算 / 志愿填报器 / 查大学概率 / 智能选大学 / 后端推荐请求）都能用。
 */

import { computed, reactive, watch } from "vue";
import { rankOfScore, beatPercent, normalizeSubjectType, subjectTypeText } from "./scoreModel";
import { readStoredAuth } from "./recommendation";

const STORAGE_KEY = "zhiyuan_exam_profile";

export const PROVINCES = [
  "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江", "上海", "江苏",
  "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南", "广东", "广西",
  "海南", "重庆", "四川", "贵州", "云南", "陕西", "甘肃", "青海", "宁夏", "新疆"
];
export const FIRST_SUBJECTS = ["物理", "历史"];
export const SECOND_SUBJECTS = ["化学", "生物", "政治", "地理"];
export const BATCHES = ["本科批", "专科批"];
export const GRADES = ["高三", "高二", "高一"];
export const ENTRANT_TYPES = [
  { key: "general", label: "普通类", icon: "学" },
  { key: "art", label: "艺术类", icon: "艺" }
];

const DEFAULTS = {
  entrantType: "general",
  province: "湖南",
  grade: "高三",
  degreeType: "本科",
  firstSubject: "物理",
  secondSubjects: ["化学", "生物"],
  score: null,
  batch: "本科批",
  // 用户手填位次时用（留空则由分数自动推算）
  manualRank: null,
  // 是否已由用户主动确认过一次（决定首页/填报页要不要提示「请先填写高考信息」）
  confirmed: false
};

function readStored() {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
    if (!raw || typeof raw !== "object") return {};
    return raw;
  } catch {
    return {};
  }
}

const profile = reactive({ ...DEFAULTS, ...readStored() });

// 兜底：历史脏数据校正
if (!Array.isArray(profile.secondSubjects)) profile.secondSubjects = [...DEFAULTS.secondSubjects];
if (!FIRST_SUBJECTS.includes(profile.firstSubject)) profile.firstSubject = DEFAULTS.firstSubject;
if (!PROVINCES.includes(profile.province)) profile.province = DEFAULTS.province;

watch(
  profile,
  (value) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...value }));
    } catch {
      /* localStorage 不可用时静默降级 */
    }
  },
  { deep: true }
);

/* ══════ 派生值（全站统一口径，来自 scoreModel） ══════ */

export const subjectType = computed(() => normalizeSubjectType(profile.firstSubject));

/** 有效分数：非法/未填返回 null（界面据此显示「请先填分数」而不是编造数字） */
export const score = computed(() => {
  const value = Number(profile.score);
  return Number.isFinite(value) && value > 0 && value <= 750 ? value : null;
});

/** 位次：优先用户手填，否则由分数推算 */
export const rank = computed(() => {
  const manual = Number(profile.manualRank);
  if (Number.isFinite(manual) && manual > 0) return manual;
  return rankOfScore(score.value, { province: profile.province, subjectType: subjectType.value });
});

export const percent = computed(() => beatPercent(score.value));

/** 信息是否完整到可以测算 */
export const isReady = computed(() => score.value != null);

/** 选科文案，如「物理 / 化学 / 生物」 */
export const subjectsText = computed(() =>
  [profile.firstSubject, ...(profile.secondSubjects || [])].filter(Boolean).join(" / ")
);

/** 一句话摘要，用于各页面顶部的考生信息条 */
export const summaryText = computed(() => {
  const parts = [profile.province, subjectsText.value];
  if (score.value != null) parts.push(`${score.value} 分`);
  if (rank.value != null) parts.push(`位次约 ${rank.value.toLocaleString("zh-CN")}`);
  parts.push(profile.batch);
  return parts.filter(Boolean).join(" · ");
});

/* ══════ 读写 API ══════ */

export function useExamProfile() {
  return {
    profile,
    score,
    rank,
    percent,
    subjectType,
    subjectsText,
    summaryText,
    isReady,
    setScore,
    setFirstSubject,
    toggleSecondSubject,
    patchProfile,
    confirmProfile,
    resetProfile,
    syncFromAuth,
    toRecommendationRequest
  };
}

export function patchProfile(patch = {}) {
  Object.entries(patch).forEach(([key, value]) => {
    if (value === undefined) return;
    profile[key] = value;
  });
  return profile;
}

export function setScore(value) {
  const num = Number(value);
  profile.score = Number.isFinite(num) && num > 0 ? Math.min(750, Math.round(num)) : null;
  // 分数变了，之前手填的位次失效，回到自动推算
  profile.manualRank = null;
  return profile.score;
}

export function setFirstSubject(value) {
  if (!FIRST_SUBJECTS.includes(value)) return;
  profile.firstSubject = value;
}

/** 再选科目最多 2 门 */
export function toggleSecondSubject(value) {
  const list = [...(profile.secondSubjects || [])];
  const index = list.indexOf(value);
  if (index >= 0) list.splice(index, 1);
  else if (list.length < 2) list.push(value);
  else return false;
  profile.secondSubjects = list;
  return true;
}

export function confirmProfile() {
  profile.confirmed = true;
  return profile;
}

export function resetProfile() {
  patchProfile({ ...DEFAULTS, secondSubjects: [...DEFAULTS.secondSubjects] });
}

/**
 * 登录用户资料 → 考生信息（只填补空缺，不覆盖用户在页面上刚填的值）
 * 让「登录时填过的分数/省份/科类」也能直接被填报器用上。
 */
export function syncFromAuth(force = false) {
  const user = readStoredAuth()?.user;
  if (!user) return profile;
  if (user.examProvince && (force || !profile.confirmed)) {
    if (PROVINCES.includes(user.examProvince)) profile.province = user.examProvince;
  }
  if (user.subjectType && (force || !profile.confirmed)) {
    profile.firstSubject = subjectTypeText(user.subjectType);
  }
  if ((force || profile.score == null) && user.score != null && user.score !== "") {
    setScore(user.score);
  }
  return profile;
}

/** 组装后端 /api/recommendations 请求体，保证前端表单与后端字段严格对应 */
export function toRecommendationRequest(extra = {}) {
  return {
    score: score.value,
    province: profile.province,
    subjectType: subjectType.value,
    recommendationMode: "SCHOOL_FIRST",
    majorKeyword: null,
    ...extra
  };
}

export { profile };
