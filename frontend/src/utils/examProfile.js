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

import { computed, reactive, ref, watch } from "vue";
import { normalizeSubjectType, subjectTypeText } from "./scoreModel.js";
import { readStoredAuth, saveStoredAuth } from "./recommendation.js";
import { fetchRankLookup } from "./scoreRankApi.js";

const STORAGE_KEY = "zhiyuan_exam_profile";

/* 只暴露当前具备完整院校线、专业线和一分一段覆盖的考试省份。 */
export const PROVINCES = ["湖南"];
export const FIRST_SUBJECTS = ["物理", "历史"];
export const SECOND_SUBJECTS = ["化学", "生物", "政治", "地理"];
export const BATCHES = ["本科批", "专科批"];
export const GRADES = ["高三", "高二", "高一"];

const DEFAULTS = {
  province: "湖南",
  grade: "高三",
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

/* ══════ 改动同步：本地持久化 + 已登录时回写后端 ══════
 * 分数/省份/科类是全局权限数据：登录时确定，志愿填报页修改后必须全站生效，
 * 且后端用户档案（推荐接口、AI 免文本解析的兜底分数）也要同步更新。
 */
let syncTimer = null;
let suppressAuthEcho = false;

function scheduleAuthSync() {
  if (suppressAuthEcho) return;
  const auth = readStoredAuth();
  if (!auth?.token) return; // 未登录：仅本地生效
  if (syncTimer) clearTimeout(syncTimer);
  syncTimer = setTimeout(async () => {
    try {
      const response = await fetch("/api/auth/profile", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${auth.token}` },
        body: JSON.stringify({
          score: score.value,
          subjectType: subjectType.value,
          examProvince: profile.province
        })
      });
      if (!response.ok) return;
      const data = await response.json();
      if (data?.token) {
        const user = { ...data };
        delete user.token;
        saveStoredAuth({ token: data.token, user });
        applyAuthenticatedProfile(user);
      }
    } catch {
      /* 离线 / 会话过期：静默降级，本地档案仍然全站生效 */
    }
  }, 800);
}

watch(
  profile,
  (value) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...value }));
    } catch {
      /* localStorage 不可用时静默降级 */
    }
    scheduleAuthSync();
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

const resolvedRank = ref(null);
export const rankLoading = ref(false);
export const rankError = ref("");
export const rankSource = ref("NONE");
export const rankSourceLabel = ref("暂无位次数据");
export const rankMappingYear = ref(null);
let rankRequestVersion = 0;

/** 位次：用户手填值优先，否则只接受后端一分一段查询结果。 */
export const rank = computed(() => {
  const manual = Number(profile.manualRank);
  if (Number.isFinite(manual) && manual > 0) return manual;
  return resolvedRank.value;
});

export async function refreshRank() {
  const requestVersion = ++rankRequestVersion;
  const manual = Number(profile.manualRank);
  if (Number.isFinite(manual) && manual > 0) {
    resolvedRank.value = null;
    rankLoading.value = false;
    rankError.value = "";
    rankSource.value = "PROVIDED";
    rankSourceLabel.value = "考生手动填写";
    rankMappingYear.value = null;
    return manual;
  }
  if (score.value == null || !profile.province || !subjectType.value || typeof window === "undefined") {
    resolvedRank.value = null;
    rankLoading.value = false;
    rankError.value = "";
    rankSource.value = "NONE";
    rankSourceLabel.value = "暂无位次数据";
    rankMappingYear.value = null;
    return null;
  }

  rankLoading.value = true;
  rankError.value = "";
  try {
    const result = await fetchRankLookup(profile.province, subjectType.value, score.value);
    if (requestVersion !== rankRequestVersion) return rank.value;
    resolvedRank.value = result.rank;
    rankSource.value = result.rankSource;
    rankSourceLabel.value = result.rankSourceLabel;
    rankMappingYear.value = result.mappingYear;
    if (result.rank == null) rankError.value = `${profile.province}${profile.firstSubject}类暂无位次数据`;
    return result.rank;
  } catch (error) {
    if (requestVersion !== rankRequestVersion) return rank.value;
    resolvedRank.value = null;
    rankSource.value = "NONE";
    rankSourceLabel.value = "暂无位次数据";
    rankMappingYear.value = null;
    rankError.value = String(error?.message || "位次查询失败");
    return null;
  } finally {
    if (requestVersion === rankRequestVersion) rankLoading.value = false;
  }
}

watch(
  [score, () => profile.province, subjectType, () => profile.manualRank],
  () => { void refreshRank(); },
  { immediate: true }
);

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
    rankLoading,
    rankError,
    rankSource,
    rankSourceLabel,
    rankMappingYear,
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
    refreshRank,
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
 * 登录用户资料 → 考生信息。登录状态下，后端用户档案是分数、省份和科类的
 * 权威来源；本地 confirmed 只能控制界面提示，不能阻止服务器新值覆盖旧缓存。
 */
export function syncFromAuth() {
  const user = readStoredAuth()?.user;
  if (!user) return profile;
  return applyAuthenticatedProfile(user);
}

export function applyAuthenticatedProfile(user) {
  if (!user) return profile;
  suppressAuthEcho = true;
  try {
    if (user.examProvince) {
      if (PROVINCES.includes(user.examProvince)) profile.province = user.examProvince;
    }
    if (user.subjectType) {
      profile.firstSubject = subjectTypeText(user.subjectType);
    }
    if (user.score != null && user.score !== "") {
      setScore(user.score);
    } else {
      profile.score = null;
      profile.manualRank = null;
    }
    profile.confirmed = Boolean(user.examProvince && user.subjectType && user.score != null && user.score !== "");
  } finally {
    // watch 回调在下一个微任务触发，稍晚一点再解除抑制
    setTimeout(() => {
      suppressAuthEcho = false;
    }, 0);
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
