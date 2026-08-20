import { SCHOOLS, MAJORS } from "./exploreData";
import {
  admissionProbability,
  rankOfScore,
  scoreOfRank,
  strategyOf as strategyOfProbability,
  segKeyOfBackendStrategy,
  backendStrategyOfSegKey
} from "./scoreModel";

/**
 * ═════ 志愿表核心：院校录取数据 + 概率模型 + 推荐列表↔填报器桥接 ═════
 *
 * 【本次改造】原先这里的 calLine / probOf 是与分数无关的假公式（500 + (21-id)*10），
 * 与 SchoolsView、ChooseView、VolunteerSheet 里又各自一套，共 4 套矛盾数据。
 * 现在统一为：
 *   院校实力 → 省内最低位次锚点 →（scoreModel 一分一段曲线）→ 最低分
 * 所以「位次」和「分数」永远自洽，概率也就能解释得通。
 * 如果数据里已经带了后端真实录取线（cutoffScore / minRank），则优先用真实值。
 */

export const TOTAL = 45;
export const SEGMENTS = [
  { key: "rush", label: "冲刺志愿", range: [0, 15], cls: "rush" },
  { key: "safe", label: "稳妥志愿", range: [15, 30], cls: "safe" },
  { key: "guard", label: "保底志愿", range: [30, 45], cls: "guard" }
];

const STORE_KEY = "zhiyuan_volunteer_sheets";
const CURRENT_KEY = "zhiyuan_volunteer_current";
const CURRENT_YEAR = 2026;

export function nameHash(name) {
  const s = String(name || "");
  let h = 0;
  for (let i = 0; i < s.length; i += 1) h = (h * 31 + s.charCodeAt(i)) % 9973;
  return h || 1;
}

function firstFinite(...values) {
  for (const value of values) {
    const num = Number(value);
    if (value !== null && value !== "" && Number.isFinite(num)) return num;
  }
  return null;
}

/* 院校对象统一为 {id, name, type, province}；推荐结果里的外部院校给合成 id，
   同时把后端带回的真实录取数据透传下去，供 schoolCutoff 优先使用 */
export function normalizeSchoolLike(schoolLike) {
  if (!schoolLike) return null;
  const name = schoolLike.name || schoolLike.universityName || schoolLike.schoolName || "";
  const realCutoff = firstFinite(schoolLike.cutoffScore, schoolLike.minScore, schoolLike.lowestScore);
  const realRank = firstFinite(schoolLike.minRank, schoolLike.lowestRank);
  const matched = SCHOOLS.find((s) => s.name === name);
  if (matched) {
    return realCutoff == null && realRank == null
      ? matched
      : { ...matched, cutoffScore: realCutoff, minRank: realRank };
  }
  const id = schoolLike.id ?? schoolLike.schoolId ?? schoolLike.universityId ?? 1000 + (nameHash(name) % 890);
  return {
    id,
    name: name || "未知院校",
    type: schoolLike.type || "综合类",
    province: schoolLike.province || schoolLike.universityProvince || "",
    cutoffScore: realCutoff,
    minRank: realRank,
    synthetic: true
  };
}

/** 院校实力排序（1 = 最强），本地库用 id，外部院校用名字 hash 稳定落到中后段 */
function tierIndexOf(school) {
  if (!school.synthetic) return school.id;
  return 6 + (nameHash(school.name) % 14);
}

/**
 * 院校（某省、某科类、某年份）录取数据——全站唯一口径
 * @returns {{minRank:number, score:number, year:number, source:"backend"|"model"}}
 */
export function schoolCutoff(schoolLike, { province = "", subjectType = "PHYSICS", yearsAgo = 0 } = {}) {
  const school = normalizeSchoolLike(schoolLike);
  if (!school) return null;
  const opts = { province, subjectType };

  // 后端真实数据优先（只有当年数据，yearsAgo>0 时仍用模型向前推）
  if (yearsAgo === 0) {
    const realScore = firstFinite(school.cutoffScore);
    const realRank = firstFinite(school.minRank);
    if (realScore != null || realRank != null) {
      return {
        score: realScore ?? scoreOfRank(realRank, opts),
        minRank: realRank ?? rankOfScore(realScore, opts),
        year: CURRENT_YEAR - 1,
        source: "backend"
      };
    }
  }

  const seed = school.synthetic ? nameHash(school.name) : school.id * 7;
  const baseRank = 200 * Math.pow(1.28, Math.max(0, tierIndexOf(school) - 1));
  const wobble = 1 + ((seed % 7) - 3) * 0.03; // ±9% 稳定拖动，同一院校永远一样
  const yearFactor = 1 + yearsAgo * 0.06; // 年份越早，门槛位次略靠后
  const minRank = Math.max(30, Math.round(baseRank * wobble * yearFactor));
  return {
    minRank,
    score: scoreOfRank(minRank, opts),
    year: CURRENT_YEAR - 1 - yearsAgo,
    source: "model"
  };
}

/** 近 N 年录取数据（院校详情页 / 填报卡片的三年对比） */
export function cutoffHistory(schoolLike, opts = {}, years = 3) {
  return Array.from({ length: years }, (_, index) => schoolCutoff(schoolLike, { ...opts, yearsAgo: index }));
}

/** 院校参考最低分（兼容旧调用） */
export function calLine(schoolLike, opts = {}) {
  const cutoff = schoolCutoff(schoolLike, opts);
  return cutoff ? cutoff.score : null;
}

/**
 * 录取概率：完全走 scoreModel（= 后端 RecommendationPolicyService 的算法）
 * @returns {number|null} 没有分数时返回 null，界面应提示「填分数测概率」而不是编个数字
 */
export function probOf(schoolLike, score, opts = {}) {
  return probDetailOf(schoolLike, score, opts).probability;
}

/** 概率明细（带分差/位次差，用于界面上把概率解释清楚） */
export function probDetailOf(schoolLike, score, opts = {}) {
  const { province = "", subjectType = "PHYSICS", userRank = null } = opts;
  const cutoff = schoolCutoff(schoolLike, { province, subjectType });
  if (!cutoff) {
    return { probability: null, scoreGap: null, rankGap: null, basis: "NONE", cutoff: null };
  }
  const result = admissionProbability({
    userScore: score,
    userRank,
    cutoffScore: cutoff.score,
    minRank: cutoff.minRank,
    province,
    subjectType
  });
  return { ...result, cutoff };
}

/**
 * 概率 → 冲/稳/保分档（阀值复用 scoreModel，与后端一致）。
 * 志愿表只有 rush/safe/guard 三种段颜色，所以把 scoreModel 的
 * risk（<35%）与 unknown（无分数）归到 rush，避免出现无样式的 class。
 */
export function strategyOf(prob) {
  return strategyOfProbability(prob);
}

export { segKeyOfBackendStrategy, backendStrategyOfSegKey };

/* 每所院校的备选专业（稳定伪随机：理工校偏工学），返回专业对象列表 */
export function majorDetailsOfSchool(schoolLike) {
  const school = normalizeSchoolLike(schoolLike);
  const pool = MAJORS.filter((m) => m.level === 0);
  const picked = pool.filter((m, i) => {
    const hit = ((i + 1) * 7 + school.id * 3) % 5 < 2;
    const engBias = school.type === "理工类" && m.category === "工学";
    const genBias = school.type === "综合类";
    return engBias || (hit && (genBias || m.category !== "工学"));
  });
  const list = picked.length >= 6 ? picked : pool.filter((m, i) => (i + school.id) % 4 === 0);
  return list.slice(0, 12);
}

export function majorsOfSchool(schoolLike) {
  return majorDetailsOfSchool(schoolLike).map((m) => m.name);
}

/**
 * 专业组录取线：在院校线基础上根据专业热度浮动（院校详情页专业表用）
 */
export function majorCutoff(schoolLike, major, opts = {}) {
  const base = schoolCutoff(schoolLike, opts);
  if (!base) return null;
  const heat = nameHash(major?.name || major) % 21; // 0~20
  const offset = Math.round((heat - 8) * 1.4); // 热门专业高于院校线
  const score = Math.max(200, Math.min(750, base.score + offset));
  return {
    score,
    minRank: rankOfScore(score, { province: opts.province, subjectType: opts.subjectType }),
    year: base.year
  };
}

/* ===== 进行中志愿表（跳页共享、刷新不丢） ===== */
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
    /* ignore */
  }
}

export function clearCurrentSheet() {
  localStorage.removeItem(CURRENT_KEY);
}

/** 段内第一个空位；段满则落到全局第一个空位 */
export function appendToCurrentSheet(slot, strategyKey) {
  const segKey = strategyKey === "guarantee" ? "guard" : strategyKey;
  const seg = SEGMENTS.find((s) => s.key === segKey) || SEGMENTS[1];
  const current = readCurrentSheet() || Array.from({ length: TOTAL }, () => null);
  for (let i = seg.range[0]; i < seg.range[1]; i += 1) {
    if (!current[i]) {
      current[i] = slot;
      writeCurrentSheet(current);
      return { ok: true, position: i + 1, segLabel: seg.label };
    }
  }
  for (let i = 0; i < TOTAL; i += 1) {
    if (!current[i]) {
      current[i] = slot;
      writeCurrentSheet(current);
      return { ok: true, position: i + 1, segLabel: "其他空位", fallback: true };
    }
  }
  return { ok: false, message: "志愿表已满，请先清理志愿位" };
}

export function currentSheetCount() {
  const cur = readCurrentSheet();
  return cur ? cur.filter(Boolean).length : 0;
}

/** 志愿位序号 → 所属段 */
export function segmentOfIndex(index) {
  return SEGMENTS.find((seg) => index >= seg.range[0] && index < seg.range[1]) || SEGMENTS[2];
}

export { STORE_KEY, CURRENT_KEY };
