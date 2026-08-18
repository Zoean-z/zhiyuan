/**
 * ══════════════════════════════════════════════════════════════
 *  统一「分数 ↔ 位次 ↔ 录取概率」模型（全站唯一数据口径）
 * ══════════════════════════════════════════════════════════════
 *
 * 改造前的问题：全站有 4 套互相矛盾的换算公式
 *   - HomeView：      rank = 780000 * (1 - score/760) ^ 1.6
 *   - VolunteerView： rank = (720 - score) * 240
 *   - RecommendSchoolRow：rank = (720 - score) * 240
 *   - SchoolsView：   概率 = 42 + (school.id * 37) % 52   ← 与分数完全无关
 * 导致「上一页填的分数没用上」「概率是啥看不懂」。
 *
 * 现在：所有页面都只能从这里取数，保证同一分数在任何页面得到同一位次、同一概率。
 *
 * 概率算法与后端 RecommendationPolicyService.java 1:1 对齐（见下方 §2），
 * 所以「本地推算的概率」与「后端返回的 admissionProbability」口径一致，
 * 前后端联调时不会出现两个数字打架。
 */

/* ══════════════════════════════════════════════════════════════
 * §1  分数 → 位次（一分一段曲线）
 * ══════════════════════════════════════════════════════════════
 * 用「分数线以上考生占比」锚点做单调分段线性插值。
 * 锚点取自公开一分一段表的典型形态（新高考物理类），
 * 只做演示用，替换成真实一分一段表时只需改 TAIL_ANCHORS。
 */

// [分数, 该分数及以上考生占全科类考生的比例]，分数降序
const TAIL_ANCHORS = [
  [750, 0.00002],
  [720, 0.00010],
  [700, 0.00060],
  [680, 0.00200],
  [660, 0.00600],
  [640, 0.01400],
  [620, 0.02800],
  [600, 0.05000],
  [580, 0.08200],
  [560, 0.12500],
  [540, 0.18000],
  [520, 0.24800],
  [500, 0.32800],
  [480, 0.41800],
  [460, 0.51500],
  [440, 0.61200],
  [420, 0.70300],
  [400, 0.78300],
  [380, 0.84800],
  [360, 0.89800],
  [340, 0.93400],
  [300, 0.97200],
  [200, 0.99600],
  [100, 1.0],
  [0, 1.0]
];

// 各省高考人数量级（演示值，单位：人）
const PROVINCE_CANDIDATES = {
  河南: 600000, 广东: 480000, 山东: 420000, 四川: 420000, 河北: 400000,
  湖南: 360000, 安徽: 350000, 江苏: 340000, 湖北: 320000, 浙江: 300000,
  江西: 280000, 广西: 260000, 云南: 240000, 贵州: 230000, 陕西: 220000,
  福建: 210000, 山西: 200000, 重庆: 190000, 辽宁: 160000, 黑龙江: 140000,
  吉林: 120000, 甘肃: 120000, 新疆: 100000, 内蒙古: 100000, 海南: 60000,
  天津: 60000, 北京: 60000, 宁夏: 50000, 上海: 50000, 青海: 30000, 西藏: 30000
};
const DEFAULT_CANDIDATES = 300000;

// 首选科目分流占比
const PHYSICS_SHARE = 0.62;
const HISTORY_SHARE = 0.38;

/** 科类归一化：接受 "PHYSICS" / "HISTORY" / "物理" / "历史" / "物理类" / "历史类" */
export function normalizeSubjectType(subjectType) {
  const text = String(subjectType || "").toUpperCase();
  if (text.includes("HISTORY") || String(subjectType || "").includes("历史")) return "HISTORY";
  return "PHYSICS";
}

export function subjectTypeText(subjectType) {
  return normalizeSubjectType(subjectType) === "HISTORY" ? "历史" : "物理";
}

/** 某省某科类的考生总数 */
export function totalCandidates(province, subjectType) {
  const base = PROVINCE_CANDIDATES[String(province || "").trim()] || DEFAULT_CANDIDATES;
  const share = normalizeSubjectType(subjectType) === "HISTORY" ? HISTORY_SHARE : PHYSICS_SHARE;
  return Math.round(base * share);
}

/** 分数线以上考生占比（0~1），单调不增 */
export function tailRatio(score) {
  const value = Number(score);
  if (!Number.isFinite(value)) return null;
  if (value >= TAIL_ANCHORS[0][0]) return TAIL_ANCHORS[0][1];
  for (let i = 0; i < TAIL_ANCHORS.length - 1; i += 1) {
    const [highScore, highRatio] = TAIL_ANCHORS[i];
    const [lowScore, lowRatio] = TAIL_ANCHORS[i + 1];
    if (value <= highScore && value >= lowScore) {
      const span = highScore - lowScore;
      if (span <= 0) return highRatio;
      const t = (highScore - value) / span;
      return highRatio + (lowRatio - highRatio) * t;
    }
  }
  return 1;
}

/**
 * 分数 → 全省位次（历史类/物理类分开排名）
 * @returns {number|null} 位次；分数非法时返回 null
 */
export function rankOfScore(score, { province = "", subjectType = "PHYSICS" } = {}) {
  const value = Number(score);
  if (!Number.isFinite(value) || value <= 0 || value > 750) return null;
  const ratio = tailRatio(value);
  if (ratio == null) return null;
  return Math.max(1, Math.round(ratio * totalCandidates(province, subjectType)));
}

/** 位次 → 等效分数（rankOfScore 的反函数，二分查找） */
export function scoreOfRank(rank, { province = "", subjectType = "PHYSICS" } = {}) {
  const target = Number(rank);
  if (!Number.isFinite(target) || target <= 0) return null;
  let low = 100;
  let high = 750;
  while (low < high) {
    const mid = Math.ceil((low + high) / 2);
    const midRank = rankOfScore(mid, { province, subjectType });
    if (midRank != null && midRank <= target) high = mid - 1;
    else low = mid;
  }
  return low;
}

/** 某一分数段的人数（一分一段表的「本段人数」） */
export function segmentCount(score, opts = {}) {
  const higher = rankOfScore(Number(score) + 1, opts);
  const current = rankOfScore(score, opts);
  if (current == null) return 0;
  return Math.max(0, current - (higher == null ? 0 : higher));
}

/** 超过本省多少百分比的考生 */
export function beatPercent(score, opts = {}) {
  const ratio = tailRatio(score);
  if (ratio == null) return null;
  return Math.min(99.9, Math.max(0.1, Math.round((1 - ratio) * 1000) / 10));
}

/* ══════════════════════════════════════════════════════════════
 * §2  录取概率（与后端 RecommendationPolicyService.java 完全一致）
 * ══════════════════════════════════════════════════════════════
 *
 * 后端算法（application.yml: recommendation.scoring）：
 *   1) 分差 scoreGap = 我的分数 - 院校去年最低分
 *      位次差 rankGap = 院校去年最低位次 - 我的位次   （正数 = 我更靠前 = 更稳）
 *   2) 分别把两个 gap 映射成概率：
 *      位次概率  rankGap < -3000            → 不推荐（null）
 *               -3000 ~ 1000               → 线性映射到 35~54（冲）
 *                1001 ~ 10000              → 线性映射到 55~74（稳）
 *               > 10000                    → 75 + (rankGap-10000)/1500，上限 96（保）
 *      分数概率  scoreGap < -10             → 不推荐（null）
 *                 -10 ~ 5                  → 线性映射到 35~54（冲）
 *                   6 ~ 20                 → 线性映射到 55~74（稳）
 *                 > 20                     → 75 + (scoreGap-20)，上限 96（保）
 *   3) 加权融合：概率 = 位次概率 * 0.75 + 分数概率 * 0.25
 *      （只有一个可算时取该值；位次数据优先，因为位次跨年可比）
 *   4) 概率 < 35 直接判定为「风险过高，不进入推荐」
 *   5) 分档：35~54 冲 / 55~74 稳 / 75~100 保
 */

export const SCORING = {
  minimumProbability: 35,
  rankWeight: 0.75,
  scoreWeight: 0.25,
  rush: { min: 35, max: 54 },
  safe: { min: 55, max: 74 },
  guarantee: { min: 75, max: 100 }
};

const MIN_RANK_GAP = -3000;
const RUSH_MAX_RANK_GAP = 1000;
const SAFE_MAX_RANK_GAP = 10000;
const MIN_SCORE_GAP = -10;
const RUSH_MAX_SCORE_GAP = 5;
const SAFE_MAX_SCORE_GAP = 20;

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function scale(value, sourceMin, sourceMax, targetMin, targetMax) {
  if (sourceMin >= sourceMax) return clamp(targetMin, 0, 100);
  if (value <= sourceMin) return clamp(targetMin, 0, 100);
  if (value >= sourceMax) return clamp(targetMax, 0, 100);
  const ratio = (value - sourceMin) / (sourceMax - sourceMin);
  return clamp(Math.round(targetMin + ratio * (targetMax - targetMin)), 0, 100);
}

/** 位次差 → 概率（后端 computeRankProbability 的等价实现） */
export function rankProbability(rankGap) {
  if (rankGap == null || !Number.isFinite(Number(rankGap))) return null;
  const gap = Number(rankGap);
  if (gap < MIN_RANK_GAP) return null;
  if (gap <= RUSH_MAX_RANK_GAP) {
    return scale(gap, MIN_RANK_GAP, RUSH_MAX_RANK_GAP, SCORING.rush.min, SCORING.rush.max);
  }
  if (gap <= SAFE_MAX_RANK_GAP) {
    return scale(gap, RUSH_MAX_RANK_GAP + 1, SAFE_MAX_RANK_GAP, SCORING.safe.min, SCORING.safe.max);
  }
  return clamp(
    SCORING.guarantee.min + Math.floor((gap - SAFE_MAX_RANK_GAP) / 1500),
    SCORING.guarantee.min,
    96
  );
}

/** 分差 → 概率（后端 computeScoreProbability 的等价实现） */
export function scoreProbability(scoreGap) {
  if (scoreGap == null || !Number.isFinite(Number(scoreGap))) return null;
  const gap = Number(scoreGap);
  if (gap < MIN_SCORE_GAP) return null;
  if (gap <= RUSH_MAX_SCORE_GAP) {
    return scale(gap, MIN_SCORE_GAP, RUSH_MAX_SCORE_GAP, SCORING.rush.min, SCORING.rush.max);
  }
  if (gap <= SAFE_MAX_SCORE_GAP) {
    return scale(gap, RUSH_MAX_SCORE_GAP + 1, SAFE_MAX_SCORE_GAP, SCORING.safe.min, SCORING.safe.max);
  }
  return clamp(SCORING.guarantee.min + (gap - SAFE_MAX_SCORE_GAP), SCORING.guarantee.min, 96);
}

/**
 * 录取概率主入口。
 * @param {object} input
 * @param {number} input.userScore   我的分数
 * @param {number} input.userRank    我的位次（缺省用 userScore 推算）
 * @param {number} input.cutoffScore 院校/专业组去年最低分
 * @param {number} input.minRank     院校/专业组去年最低位次
 * @returns {{
 *   probability: number|null, basis: string, scoreGap: number|null, rankGap: number|null,
 *   rankProbability: number|null, scoreProbability: number|null, recommended: boolean
 * }}
 */
export function admissionProbability({
  userScore = null,
  userRank = null,
  cutoffScore = null,
  minRank = null,
  province = "",
  subjectType = "PHYSICS"
} = {}) {
  const score = Number.isFinite(Number(userScore)) ? Number(userScore) : null;
  const resolvedRank = Number.isFinite(Number(userRank))
    ? Number(userRank)
    : rankOfScore(score, { province, subjectType });
  const line = Number.isFinite(Number(cutoffScore)) ? Number(cutoffScore) : null;
  const lineRank = Number.isFinite(Number(minRank))
    ? Number(minRank)
    : rankOfScore(line, { province, subjectType });

  const scoreGap = score == null || line == null ? null : score - line;
  const rankGap = resolvedRank == null || lineRank == null ? null : lineRank - resolvedRank;

  const rankProb = rankProbability(rankGap);
  const scoreProb = scoreProbability(scoreGap);

  let probability = null;
  let basis = "NONE";
  if (rankProb != null && scoreProb != null) {
    probability = clamp(
      Math.round(rankProb * SCORING.rankWeight + scoreProb * SCORING.scoreWeight),
      0,
      100
    );
    basis = "RANK";
  } else if (rankProb != null) {
    probability = rankProb;
    basis = "RANK";
  } else if (scoreProb != null) {
    probability = scoreProb;
    basis = "SCORE";
  }

  return {
    probability,
    basis,
    scoreGap,
    rankGap,
    userRank: resolvedRank,
    minRank: lineRank,
    rankProbability: rankProb,
    scoreProbability: scoreProb,
    recommended: probability != null && probability >= SCORING.minimumProbability
  };
}

/** 概率分档（与后端 StrategyType 对齐；低于 35 单独给出「风险」档，前端筛选用） */
export function strategyOf(probability) {
  const value = Number(probability);
  if (!Number.isFinite(value)) {
    return { key: "unknown", label: "—", full: "待测算", color: "#98a2b3" };
  }
  if (value >= SCORING.guarantee.min) {
    return { key: "guard", label: "保", full: "保底", color: "#21a366" };
  }
  if (value >= SCORING.safe.min) {
    return { key: "safe", label: "稳", full: "稳妥", color: "#1890ff" };
  }
  if (value >= SCORING.rush.min) {
    return { key: "rush", label: "冲", full: "冲刺", color: "#ff6600" };
  }
  return { key: "risk", label: "险", full: "风险", color: "#e5484d" };
}

/** 后端 strategy 枚举（RUSH/SAFE/GUARANTEE）↔ 前端段 key（rush/safe/guard） */
export function segKeyOfBackendStrategy(strategy) {
  const text = String(strategy || "").toUpperCase();
  if (text.includes("RUSH")) return "rush";
  if (text.includes("GUARANTEE")) return "guard";
  if (text.includes("SAFE")) return "safe";
  return "safe";
}

export function backendStrategyOfSegKey(segKey) {
  if (segKey === "rush") return "RUSH";
  if (segKey === "guard") return "GUARANTEE";
  return "SAFE";
}

/** 概率一句话解释（界面上直接告诉用户这个数是怎么来的） */
export function probabilityExplain({ probability, scoreGap, rankGap, basis }) {
  if (probability == null) return "缺少院校往年录取数据，暂时无法测算";
  const parts = [];
  if (rankGap != null) {
    parts.push(rankGap >= 0
      ? `你的位次比该校去年最低位次靠前 ${Math.abs(rankGap).toLocaleString("zh-CN")} 名`
      : `你的位次比该校去年最低位次落后 ${Math.abs(rankGap).toLocaleString("zh-CN")} 名`);
  }
  if (scoreGap != null) {
    parts.push(scoreGap >= 0 ? `分数高出 ${scoreGap} 分` : `分数低 ${Math.abs(scoreGap)} 分`);
  }
  const weight = basis === "RANK" ? "位次权重 75% + 分数权重 25%" : "仅按分差测算";
  return `${parts.join("，")}；按${weight}折算，录取概率约 ${probability}%`;
}
