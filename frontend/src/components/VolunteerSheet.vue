<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Bottom, Top, CircleCheckFilled, WarningFilled, InfoFilled, Promotion, StarFilled } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import GkSchoolLogo from "./GkSchoolLogo.vue";
import { SCHOOLS } from "../utils/exploreData";
import {
  SEGMENTS, TOTAL, calLine, cutoffHistory, majorsOfSchool, normalizeSchoolLike, probOf, readCurrentSheet, strategyOf, writeCurrentSheet
} from "../utils/volunteerCore";
import { normalizeSubjectType, rankOfScore, scoreOfRank } from "../utils/scoreModel";

const props = defineProps({
  profile: { type: Object, required: true },
  initialTab: { type: String, default: "pick" },
  initialView: { type: String, default: "detail" }
});
const router = useRouter();

/* ===== 模块切换：模拟填报（选校） / 志愿表（参考 mnzy.gaokao.cn 双模块） ===== */
const activeTab = ref(props.initialTab === "sheet" ? "sheet" : "pick");

/* ===== 志愿位（含"进行中方案"持久化，跨页共享） ===== */
const slots = ref(Array.from({ length: TOTAL }, () => null));

const restored = readCurrentSheet();
if (restored && restored.some(Boolean)) {
  slots.value = restored.map((s) => (s ? { ...s } : null));
}
watch(slots, (val) => writeCurrentSheet(val.map((s) => (s ? { ...s } : null))), { deep: true });

const filledCount = computed(() => slots.value.filter(Boolean).length);
const progress = computed(() => Math.round((filledCount.value / TOTAL) * 100));
function normalizeProbability(prob) {
  return Number.isFinite(Number(prob)) ? Number(prob) : null;
}
function probabilityText(prob) {
  const value = normalizeProbability(prob);
  if (value == null) return "待测";
  if (value <= 0) return "<1%";
  if (value >= 96) return "96%+";
  return `${value}%`;
}
function compareProbability(a, b) {
  const pa = normalizeProbability(a);
  const pb = normalizeProbability(b);
  if (pa == null && pb == null) return 0;
  if (pa == null) return 1;
  if (pb == null) return -1;
  return pa - pb;
}
const segStats = computed(() => {
  const stat = { rush: 0, safe: 0, guard: 0, unknown: 0 };
  slots.value.forEach((s) => {
    if (!s) return;
    const key = strategyOf(s.prob).key;
    if (key in stat) stat[key] += 1;
    else stat.unknown += 1;
  });
  return stat;
});

/* ===== 位次换算（与首页同源） ===== */
/**
 * 【修复】原先这两个函数是拍脑袋的幂函数 780000 * (1 - score/760)^1.6，
 * 与首页、志愿填报表单、查大学里的公式全部不一致（同一分数四个位次）。
 * 现在统一走 scoreModel（分省份 + 分科类的一分一段模型）。
 */
const modelOpts = computed(() => ({
  province: props.profile.province,
  subjectType: normalizeSubjectType(props.profile.subjects?.[0] || props.profile.subjectType)
}));
function rankOf(score) {
  return rankOfScore(score, modelOpts.value);
}
function scoreForRank(rank) {
  return scoreOfRank(rank, modelOpts.value);
}
const myRank = computed(() => Number(props.profile.rank || rankOf(props.profile.score)));

/* ===== 模拟填报：院校卡片数据（确定性派生，参考 mnzy 院校卡） ===== */
const pickStrategy = ref("all");
const pickType = ref("全部");
const sortBy = ref("概率");
const pickQuery = ref("");
const lineBounds = computed(() => {
  const lines = SCHOOLS.map((s) => calLine(s, modelOpts.value));
  return [Math.min(...lines) - 20, Math.max(...lines) + 20];
});
const scoreRange = ref([...lineBounds.value]);
watch(lineBounds, (b) => {
  scoreRange.value = [...b];
});

function schoolFacts(school) {
  const prob = probOfHere(school);
  const years = cutoffHistory(school, modelOpts.value, 3).filter(Boolean).map((cutoff) => {
    const line = cutoff.score;
    const cutoffRank = cutoff.minRank;
    const gap = myRank.value - cutoffRank;
    const equiv = scoreForRank(cutoffRank);
    return {
      year: cutoff.year,
      source: cutoff.source,
      line,
      cutoffRank,
      gap,
      gapText: gap > 0 ? `靠前 ${gap.toLocaleString("zh-CN")} 名` : `落后 ${(-gap).toLocaleString("zh-CN")} 名`,
      equiv,
      diff: equiv - Number(props.profile.score || 0),
      diffText: equiv >= Number(props.profile.score || 0) ? `高 ${equiv - Number(props.profile.score || 0)} 分` : `低 ${Number(props.profile.score || 0) - equiv} 分`
    };
  });
  return {
    school,
    prob,
    strategy: strategyOf(prob),
    line: years[0]?.line ?? calLine(school, modelOpts.value),
    years,
    majorList: majorsOfSchool(school)
  };
}

const filteredFacts = computed(() => {
  let list = SCHOOLS.map(schoolFacts).filter((f) => f.line >= scoreRange.value[0] && f.line <= scoreRange.value[1]);
  if (pickType.value !== "全部") list = list.filter((f) => f.school.type === pickType.value);
  const q = pickQuery.value.trim();
  if (q) list = list.filter((f) => f.school.name.includes(q) || f.school.province.includes(q));
  const byKey = { all: null, rush: "rush", safe: "safe", guard: "guard" };
  if (byKey[pickStrategy.value]) list = list.filter((f) => f.strategy.key === byKey[pickStrategy.value]);
  if (sortBy.value === "概率") list = [...list].sort((a, b) => compareProbability(a.prob, b.prob));
  else list = [...list].sort((a, b) => a.school.id - b.school.id);
  return list;
});
const pickCounts = computed(() => {
  const list = SCHOOLS.map(schoolFacts).filter((f) => f.line >= scoreRange.value[0] && f.line <= scoreRange.value[1]);
  return {
    all: list.length,
    rush: list.filter((f) => f.strategy.key === "rush").length,
    safe: list.filter((f) => f.strategy.key === "safe").length,
    guard: list.filter((f) => f.strategy.key === "guard").length
  };
});

const expandedId = ref(null);
function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id;
}
function inSheet(facts) {
  return slots.value.some((s) => s && s.schoolId === facts.school.id);
}
function addFromPick(facts) {
  const segKey = facts.strategy.key;
  const seg = SEGMENTS.find((s) => s.key === segKey) || SEGMENTS[1];
  let target = -1;
  for (let i = seg.range[0]; i < seg.range[1]; i += 1) {
    if (!slots.value[i]) { target = i; break; }
  }
  if (target < 0) {
    for (let i = 0; i < TOTAL; i += 1) {
      if (!slots.value[i]) { target = i; break; }
    }
  }
  if (target < 0) {
    ElMessage.warning("志愿表已满，请先清理志愿位");
    return;
  }
  slots.value[target] = {
    schoolId: facts.school.id,
    schoolName: facts.school.name,
    majorNames: facts.majorList.slice(0, 3),
    adjust: true,
    prob: facts.prob
  };
  ElMessage.success(`已加入第 ${target + 1} 志愿位（${seg.label}）`);
}
function askAbout(facts) {
  router.push({ path: "/agent", query: { q: `帮我分析${facts.school.name}：我现在${props.profile.score}分，页面判断为${facts.strategy.label}，参考概率${probabilityText(facts.prob)}，近三年参考线为${facts.years.map((y) => y.line).join("/")}。请说明数据局限，并分析是否适合放进志愿表。` } });
}

/* 下拉可选项 = 本地院校库 + 已填的外部院校（来自推荐结果投放） */
const schoolOptions = computed(() => {
  const extras = [];
  slots.value.forEach((s) => {
    if (!s || s.schoolId == null) return;
    if (SCHOOLS.some((x) => x.id === s.schoolId)) return;
    if (extras.some((x) => x.id === s.schoolId)) return;
    extras.push({
      id: s.schoolId,
      name: s.schoolName || `院校 ${s.schoolId}`,
      province: "",
      type: "综合类",
      is985: false,
      is211: false,
      isDoubleFirstClass: false
    });
  });
  return [...SCHOOLS, ...extras];
});

function probOfHere(school) {
  return probOf(school, props.profile.score, { ...modelOpts.value, userRank: myRank.value });
}

function setSchool(slot, schoolId) {
  if (!schoolId) {
    slot.schoolId = null;
    slot.schoolName = "";
    slot.majorNames = [];
    slot.prob = null;
    return;
  }
  const school = schoolOptions.value.find((s) => s.id === schoolId) || null;
  slot.schoolId = school ? school.id : null;
  slot.schoolName = school ? school.name : "";
  slot.majorNames = [];
  slot.prob = school ? probOfHere(school) : 0;
}

function schoolOf(slot) {
  if (!slot) return null;
  const matched = SCHOOLS.find((s) => s.id === slot.schoolId);
  if (matched) return matched;
  if (slot.schoolId == null && !slot.schoolName) return null;
  return normalizeSchoolLike({ id: slot.schoolId, name: slot.schoolName });
}

/* ===== 移动 / 删除 / 拖拽 ===== */
function moveSlot(idx, dir) {
  const to = idx + dir;
  if (to < 0 || to >= TOTAL) return;
  const arr = slots.value;
  [arr[idx], arr[to]] = [arr[to], arr[idx]];
}
function removeSlot(idx) {
  slots.value[idx] = null;
}

const dragIdx = ref(-1);
function onDrop(to) {
  if (dragIdx.value < 0 || dragIdx.value === to) return;
  const arr = slots.value;
  const [item] = arr.splice(dragIdx.value, 1);
  arr.splice(to, 0, item);
  dragIdx.value = -1;
}

/* ===== 智能填充 ===== */
function smartFill() {
  if (filledCount.value >= TOTAL) {
    ElMessage.info("志愿表已填满");
    return;
  }
  const ranked = SCHOOLS.map((s) => ({ school: s, prob: probOfHere(s) }))
    .filter((item) => normalizeProbability(item.prob) != null)
    .sort((a, b) => compareProbability(b.prob, a.prob));
  // 按段投放：冲→1-15 位、稳→16-30 位、保→31-45 位，段内从段首依次填
  const groups = [
    { seg: SEGMENTS[0], picks: ranked.filter((r) => r.prob < 45).slice(0, 6) }, // 冲：概率最接近45%的一批
    { seg: SEGMENTS[1], picks: ranked.filter((r) => r.prob >= 45 && r.prob < 75).slice(0, 8) }, // 稳
    { seg: SEGMENTS[2], picks: ranked.filter((r) => r.prob >= 75).slice(0, 6) } // 保
  ];
  let filledTotal = 0;
  groups.forEach(({ seg, picks }) => {
    let p = 0;
    for (let i = seg.range[0]; i < seg.range[1] && p < picks.length; i++) {
      if (slots.value[i]) continue;
      const pick = picks[p++];
      slots.value[i] = { schoolId: pick.school.id, schoolName: pick.school.name, majorNames: majorsOfSchool(pick.school).slice(0, 3), adjust: true, prob: pick.prob };
      filledTotal += 1;
    }
  });
  ElMessage.success(`已智能填充 ${filledTotal} 个志愿，可继续手动调整`);
}

/* ===== 智能排序：已填志愿按录取概率升序（冲→稳→保）重排 ===== */
function smartSort() {
  const filled = slots.value.filter(Boolean);
  if (filled.length < 2) {
    ElMessage.info("至少填写 2 个志愿后再排序");
    return;
  }
  filled.sort((a, b) => compareProbability(a.prob, b.prob));
  slots.value = Array.from({ length: TOTAL }, () => null);
  filled.forEach((s, i) => {
    slots.value[i] = s;
  });
  ElMessage.success(`已按「冲→稳→保」梯度重排 ${filled.length} 个志愿`);
}

/* ===== 导出（复制为表格文本，可粘贴 Excel） ===== */
async function exportSheet() {
  if (!filledCount.value) {
    ElMessage.warning("志愿表为空，暂无可导出内容");
    return;
  }
  const header = ["序号", "院校名称", "录取概率", "策略", "专业", "服从调剂"];
  const rows = slots.value.map((s, i) =>
    s ? [i + 1, s.schoolName, probabilityText(s.prob), strategyOf(s.prob).label, (s.majorNames || []).join("、"), s.adjust ? "是" : "否"] : null
  ).filter(Boolean);
  const text = [header, ...rows].map((r) => r.join("\t")).join("\n");
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success("志愿表已复制到剪贴板，可直接粘贴到 Excel / WPS");
  } catch {
    ElMessage.info("剪贴板不可用，请手动截图保存");
  }
}

function clearAll() {
  ElMessageBox.confirm("清空后不可恢复，是否继续？", "清空志愿表", { type: "warning" })
    .then(() => {
      slots.value = Array.from({ length: TOTAL }, () => null);
      ElMessage.success("已清空");
    })
    .catch(() => {});
}

/* ===== 风险诊断 ===== */
const diagVisible = ref(false);
const diagItems = computed(() => {
  const filled = slots.value.map((s, i) => ({ slot: s, idx: i })).filter((x) => x.slot);
  const items = [];
  if (!filled.length) {
    items.push({ level: "warn", text: "还没有填写任何志愿，建议先在「模拟填报」中添加院校，或使用「智能填充」生成初始方案" });
    return items;
  }
  const probs = filled.map((x) => normalizeProbability(x.slot.prob)).filter((x) => x != null);
  const unknownCount = filled.length - probs.length;
  // 梯度
  if (probs.length >= 2) {
    if (Math.max(...probs) - Math.min(...probs) < 15) {
      items.push({ level: "warn", text: `全部志愿录取概率集中在 ${Math.min(...probs)}%~${Math.max(...probs)}%，梯度不足，掉档风险高，建议拉开冲稳保层次` });
    } else {
      items.push({ level: "ok", text: `志愿梯度 ${Math.min(...probs)}%~${Math.max(...probs)}%，层次合理` });
    }
  } else {
    items.push({ level: "info", text: "当前可测概率不足 2 个，暂时无法判断梯度，建议补充分数或录取线数据" });
  }
  if (unknownCount) {
    items.push({ level: "info", text: `有 ${unknownCount} 个志愿暂未测出概率，当前不纳入梯度诊断` });
  }
  // 兜底
  const tail = filled.filter((x) => x.idx >= 30);
  const tailKnown = tail.map((x) => normalizeProbability(x.slot.prob)).filter((x) => x != null);
  if (tailKnown.length && Math.min(...tailKnown) < 85) {
    items.push({ level: "warn", text: "保底段（31-45 位）存在录取概率低于 85% 的志愿，兜底不牢固，建议换成把握更大的院校" });
  } else if (tail.length >= 3) {
    items.push({ level: "ok", text: "保底段兜底充分，掉档风险低" });
  } else {
    items.push({ level: "info", text: `保底段仅填 ${tail.length} 个志愿，建议至少填满 5 个高概率兜底` });
  }
  // 冲刺段
  const head = filled.filter((x) => x.idx < 15);
  const headKnown = head.map((x) => normalizeProbability(x.slot.prob)).filter((x) => x != null);
  if (headKnown.length && Math.min(...headKnown) > 70) {
    items.push({ level: "info", text: "冲刺段（1-15 位）缺少真正可冲一冲的院校，可加入 1-2 所概率 20%~45% 的目标院校" });
  }
  // 重复
  const seen = new Map();
  filled.forEach((x) => {
    const key = `${x.slot.schoolId}-${[...x.slot.majorNames].sort().join(",")}`;
    if (seen.has(key)) items.push({ level: "error", text: `第 ${seen.get(key) + 1} 位与第 ${x.idx + 1} 位志愿完全相同（同院校同专业），请删除其一` });
    else seen.set(key, x.idx + 1);
  });
  // 调剂
  const noAdjust = filled.filter((x) => !x.slot.adjust && x.slot.majorNames.length < 6);
  if (noAdjust.length) {
    items.push({ level: "info", text: `有 ${noAdjust.length} 个志愿未服从调剂且专业数不足 6 个，退档风险略高，建议开启服从调剂或补足专业` });
  }
  // 填满度
  if (filled.length < TOTAL) {
    items.push({ level: "info", text: `已填 ${filled.length}/${TOTAL} 个志愿，还有 ${TOTAL - filled.length} 个空位，多填一个多一分机会` });
  } else {
    items.push({ level: "ok", text: "45 个志愿全部填满，完整度 100%" });
  }
  return items;
});
const diagScore = computed(() => {
  const items = diagItems.value;
  if (items.some((i) => i.level === "error")) return { label: "需修正", cls: "bad" };
  if (items.some((i) => i.level === "warn")) return { label: "有风险", cls: "mid" };
  if (filledCount.value >= 20) return { label: "结构良好", cls: "good" };
  return { label: "继续完善", cls: "mid" };
});

/* ===== 志愿表视图模式：详细 / 表格 ===== */
const viewMode = ref(props.initialView === "table" ? "table" : "detail");
const tableRows = computed(() =>
  slots.value.map((s, i) => ({ idx: i + 1, slot: s })).filter((r) => r.slot)
);

/* ===== 保存方案（独立于 /plans，localStorage 持久化） ===== */
const STORE_KEY = "zhiyuan_volunteer_sheets";
const savedSheets = ref([]);
try {
  savedSheets.value = JSON.parse(localStorage.getItem(STORE_KEY) || "[]");
} catch {
  savedSheets.value = [];
}

function persistSheets() {
  localStorage.setItem(STORE_KEY, JSON.stringify(savedSheets.value));
}

function saveSheet() {
  if (!filledCount.value) {
    ElMessage.warning("志愿表为空，先填写或智能填充后再保存");
    return;
  }
  const now = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  const name = `志愿方案 ${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`;
  savedSheets.value.unshift({
    id: Date.now(),
    name,
    profile: { ...props.profile },
    slots: slots.value.filter(Boolean).length,
    detail: slots.value.map((s, i) => (s ? { idx: i + 1, ...s } : null)).filter(Boolean),
    savedAt: now.toISOString()
  });
  persistSheets();
  ElMessage.success(`已保存「${name}」`);
}

function loadSheet(sheet) {
  slots.value = Array.from({ length: TOTAL }, () => null);
  sheet.detail.forEach((d) => {
    const { idx, ...rest } = d;
    slots.value[idx - 1] = { ...rest };
  });
  ElMessage.success(`已载入「${sheet.name}」`);
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function removeSheet(id) {
  savedSheets.value = savedSheets.value.filter((s) => s.id !== id);
  persistSheets();
  ElMessage.success("已删除方案");
}

/* profile 分数变化时，本地库院校概率同步刷新；外部投放的院校保留推荐概率 */
watch(() => props.profile.score, () => {
  slots.value.forEach((s) => {
    if (!s?.schoolId) return;
    const matched = SCHOOLS.find((x) => x.id === s.schoolId);
    if (matched) s.prob = probOfHere(matched);
  });
});

defineExpose({ smartFill });
</script>

<template>
  <div class="mnz-vsheet">
    <!-- 模块 tabs（参考 mnzy 模拟填报/志愿表双模块） -->
    <nav class="mnz-tabs">
      <button type="button" :class="{ 'is-active': activeTab === 'pick' }" @click="activeTab = 'pick'">
        模拟填报<em>自选冲稳保院校</em>
      </button>
      <button type="button" :class="{ 'is-active': activeTab === 'sheet' }" @click="activeTab = 'sheet'">
        志愿表<em>{{ filledCount }} / {{ TOTAL }} 已填</em>
      </button>
    </nav>

    <!-- ════════ 模拟填报：院校选择 ════════ -->
    <template v-if="activeTab === 'pick'">
      <div class="mnz-pick__ctrl">
        <div class="mnz-pick__score">
          <span class="mnz-pick__score-label">只看推荐分数</span>
          <el-slider
            v-model="scoreRange"
            range
            :min="lineBounds[0]"
            :max="lineBounds[1]"
            :step="5"
            class="mnz-pick__slider"
          />
          <b>{{ scoreRange[0] }} - {{ scoreRange[1] }} 分</b>
        </div>
      </div>

      <div class="mnz-pick__tabs">
        <button v-for="t in [
          { key: 'all', label: '全部', n: pickCounts.all },
          { key: 'rush', label: '冲击', n: pickCounts.rush },
          { key: 'safe', label: '稳妥', n: pickCounts.safe },
          { key: 'guard', label: '保底', n: pickCounts.guard }
        ]" :key="t.key" type="button" :class="{ 'is-active': pickStrategy === t.key }" @click="pickStrategy = t.key">
          {{ t.label }} <i>{{ t.n }}</i>
        </button>
      </div>

      <div class="mnz-pick__filters">
        <div class="mnz-pick__seg">
          <button v-for="t in ['全部', '综合类', '理工类']" :key="t" type="button" :class="{ 'is-active': pickType === t }" @click="pickType = t">
            {{ t }}
          </button>
        </div>
        <div class="mnz-pick__seg">
          <button v-for="s in ['概率', '院校']" :key="s" type="button" :class="{ 'is-active': sortBy === s }" @click="sortBy = s">
            {{ s === '概率' ? '按概率' : '按院校' }}
          </button>
        </div>
        <div class="mnz-pick__search">
          <el-icon><Promotion /></el-icon>
          <input v-model="pickQuery" type="text" placeholder="输入院校 / 省份名称" />
        </div>
      </div>

      <div class="mnz-pick__list">
        <article v-for="f in filteredFacts" :key="f.school.id" class="mnz-pcard">
          <!-- 盾形概率徽章 + 校徽 -->
          <div class="mnz-pcard__badge">
            <GkSchoolLogo :school="f.school" size="sm" class="mnz-pcard__logo" />
            <span class="mnz-pcard__shield" :class="`is-${f.strategy.key}`">
              <b>{{ probabilityText(f.prob) }}</b>
              <i>{{ f.strategy.label }}</i>
            </span>
          </div>

          <div class="mnz-pcard__main">
            <div class="mnz-pcard__title">
              <h4>{{ f.school.name }}<span>[{{ String(f.school.id).padStart(2, "0") }}组]</span></h4>
              <span class="mnz-pcard__purity" title="纯净度：院校组内专业冷热均衡程度">
                纯净度
                <i v-for="n in 5" :key="n" :class="{ 'is-on': n <= Math.round(f.purity) }">★</i>
                <em>{{ f.purity }}</em>
              </span>
              <span class="mnz-pcard__disc">{{ f.disciplines }}</span>
            </div>

            <div class="mnz-pcard__tags">
              <span v-for="t in [f.school.province, f.school.type, f.school.nature, f.school.belong, f.school.is985 ? '985' : '', (f.school.is211 || f.school.isDoubleFirstClass) ? '双一流' : ''].filter(Boolean)" :key="t" class="mnz-pcard__tag">{{ t }}</span>
            </div>

            <div class="mnz-pcard__years">
              <div v-for="y in f.years" :key="y.year" class="mnz-pcard__year">
                <header>{{ y.year }}年{{ y.source === "backend" ? "录取数据" : "模型参考" }}</header>
                <p><label>{{ y.source === "backend" ? "最低分" : "参考分" }}</label><b>{{ y.line }} 分</b></p>
                <p><label>{{ y.source === "backend" ? "最低位次" : "参考位次" }}</label><b>{{ y.cutoffRank.toLocaleString("zh-CN") }} 名</b></p>
                <p><label>比我位次</label><span class="mnz-pcard__gap" :class="y.gap > 0 ? 'is-ahead' : 'is-behind'">{{ y.gapText }}</span></p>
                <p><label>等效分</label><b>{{ y.equiv }} 分</b></p>
                <p><label>等效分差</label><span :class="y.diff >= 0 ? 'is-ahead' : 'is-behind'">{{ y.diffText }}</span></p>
              </div>
            </div>

            <div class="mnz-pcard__foot">
              <button type="button" class="mnz-pcard__majors-btn" @click="toggleExpand(f.school.id)">
                可填专业({{ f.majorList.length }})
                <el-icon><Promotion /></el-icon>
              </button>
              <button type="button" class="mnz-pcard__ask" @click="askAbout(f)">问小智解读</button>
              <div v-if="expandedId === f.school.id" class="mnz-pcard__major-list">
                <span v-for="m in f.majorList" :key="m">{{ m }}</span>
              </div>
            </div>
          </div>

          <div class="mnz-pcard__side">
            <button
              type="button"
              class="mnz-pcard__add"
              :class="{ 'is-added': inSheet(f) }"
              :disabled="inSheet(f)"
              @click="addFromPick(f)"
            >
              {{ inSheet(f) ? "已添加" : "添加" }}
            </button>
            <span class="mnz-pcard__seg-name">{{ f.strategy.label }}段推荐</span>
          </div>
        </article>

        <div v-if="!filteredFacts.length" class="mnz-pick__empty">
          <p>当前筛选条件下没有合适院校</p>
          <span>试试放宽分数区间，或切换冲稳保类型</span>
        </div>
      </div>
    </template>

    <!-- ════════ 志愿表 ════════ -->
    <template v-else>
      <!-- 顶部：考生摘要 + 进度 + 操作 -->
      <div class="mnz-vsheet__bar">
        <div class="mnz-vsheet__profile">
          <strong>{{ profile.province }}</strong>
          <span>{{ profile.subjects.join("/") }}</span>
          <b>{{ profile.score }} 分</b>
          <span>位次约 {{ Number(profile.rank).toLocaleString("zh-CN") }}</span>
          <em>{{ profile.batch }}</em>
        </div>
        <div class="mnz-vsheet__progress">
          <div class="mnz-vsheet__progress-track">
            <i :style="{ width: `${progress}%` }" />
          </div>
          <span>已填 {{ filledCount }} / {{ TOTAL }}</span>
        </div>
        <div class="mnz-vsheet__ops">
          <div class="mnz-pick__seg mnz-pick__seg--view">
            <button v-for="m in ['detail', 'table']" :key="m" type="button" :class="{ 'is-active': viewMode === m }" @click="viewMode = m">
              {{ m === "detail" ? "详细模式" : "表格模式" }}
            </button>
          </div>
          <button type="button" class="mnz-vsheet__op mnz-vsheet__op--fill" @click="smartFill">智能填充</button>
          <button type="button" class="mnz-vsheet__op" @click="smartSort">智能排序</button>
          <button type="button" class="mnz-vsheet__op" @click="exportSheet">导出</button>
          <button type="button" class="mnz-vsheet__op" @click="diagVisible = !diagVisible">风险诊断</button>
          <button type="button" class="mnz-vsheet__op" @click="clearAll">清空</button>
          <button type="button" class="mnz-vsheet__op mnz-vsheet__op--save" @click="saveSheet">保存方案</button>
        </div>
      </div>

      <!-- 统计 chips -->
      <div class="mnz-vsheet__stats">
        <span class="mnz-vstat is-rush">冲 {{ segStats.rush }}</span>
        <span class="mnz-vstat is-safe">稳 {{ segStats.safe }}</span>
        <span class="mnz-vstat is-guard">保 {{ segStats.guard }}</span>
        <span class="mnz-vsheet__drag-tip">拖拽志愿行可调整顺序，志愿位置决定投档先后</span>
      </div>

      <!-- 诊断面板 -->
      <div v-if="diagVisible" class="mnz-vdiag">
        <div class="mnz-vdiag__head">
          <h4>防掉档诊断报告</h4>
          <span class="mnz-vdiag__score" :class="`is-${diagScore.cls}`">{{ diagScore.label }}</span>
          <button type="button" class="mnz-vdiag__close" @click="diagVisible = false">收起</button>
        </div>
        <ul class="mnz-vdiag__list">
          <li v-for="(item, i) in diagItems" :key="i" :class="`is-${item.level}`">
            <el-icon>
              <CircleCheckFilled v-if="item.level === 'ok'" />
              <WarningFilled v-else-if="item.level === 'warn' || item.level === 'error'" />
              <InfoFilled v-else />
            </el-icon>
            <span>{{ item.text }}</span>
          </li>
        </ul>
      </div>

      <!-- 表格模式：官方填报系统同款表格 -->
      <div v-if="viewMode === 'table'" class="mnz-table">
        <table>
          <thead>
            <tr>
              <th>序号</th>
              <th>院校名称</th>
              <th>录取概率</th>
              <th>专业</th>
              <th>服从调剂</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in tableRows" :key="r.idx">
              <td class="mnz-table__seq">{{ String(r.idx).padStart(2, "0") }}</td>
              <td class="mnz-table__school">
                <GkSchoolLogo :school="schoolOf(r.slot)" size="mini" />
                {{ r.slot.schoolName }}
              </td>
              <td>
                <span class="mnz-table__prob" :class="strategyOf(r.slot.prob).key"><em>录取率</em> {{ probabilityText(r.slot.prob) }}</span>
              </td>
              <td class="mnz-table__majors">{{ (r.slot.majorNames || []).join("、") || "—" }}</td>
              <td>{{ r.slot.adjust ? "是" : "否" }}</td>
              <td>
                <button type="button" class="mnz-table__del" @click="removeSlot(r.idx - 1)">
                  <el-icon><Delete /></el-icon>
                </button>
              </td>
            </tr>
            <tr v-if="!tableRows.length">
              <td colspan="6" class="mnz-table__empty">暂未添加志愿，可切换「详细模式」逐位填报，或使用智能填充</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 详细模式：三段志愿位 -->
      <template v-else>
        <section v-for="seg in SEGMENTS" :key="seg.key" class="mnz-vseg" :class="`is-${seg.cls}`">
          <header class="mnz-vseg__head">
            <h5>{{ seg.label }}</h5>
            <span>第 {{ seg.range[0] + 1 }} - {{ seg.range[1] }} 志愿</span>
            <em>{{ slots.slice(seg.range[0], seg.range[1]).filter(Boolean).length }} / {{ seg.range[1] - seg.range[0] }}</em>
          </header>

          <div
            v-for="n in seg.range[1] - seg.range[0]"
            :key="seg.key + n"
            class="mnz-vrow"
            :class="{ 'is-filled': !!slots[seg.range[0] + n - 1], 'is-dragover': dragIdx >= 0 && dragIdx !== seg.range[0] + n - 1 }"
            :draggable="!!slots[seg.range[0] + n - 1]"
            @dragstart="dragIdx = seg.range[0] + n - 1"
            @dragend="dragIdx = -1"
            @dragover.prevent
            @drop="onDrop(seg.range[0] + n - 1)"
          >
            <i class="mnz-vrow__seq">{{ seg.range[0] + n }}</i>

            <template v-if="slots[seg.range[0] + n - 1]">
              <el-select
                :model-value="slots[seg.range[0] + n - 1].schoolId"
                class="mnz-vrow__school"
                filterable
                @update:model-value="setSchool(slots[seg.range[0] + n - 1], $event)"
              >
                <el-option v-for="s in schoolOptions" :key="s.id" :value="s.id" :label="s.name">
                  <span class="mnz-vrow__school-name">{{ s.name }}</span>
                  <span class="mnz-vrow__school-meta">{{ s.province ? s.province + " · " : "" }}{{ s.is985 ? "985" : (s.is211 || s.isDoubleFirstClass) ? "双一流" : s.province ? "" : "推荐投放" }}</span>
                </el-option>
              </el-select>

              <el-select
                v-model="slots[seg.range[0] + n - 1].majorNames"
                class="mnz-vrow__majors"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="选择专业（最多 6 个）"
                :max-collapse-tags="2"
              >
                <el-option v-for="m in majorsOfSchool(schoolOf(slots[seg.range[0] + n - 1]))" :key="m" :label="m" :value="m" />
              </el-select>

              <button
                type="button"
                class="mnz-vrow__adjust"
                :class="{ 'is-on': slots[seg.range[0] + n - 1].adjust }"
                @click="slots[seg.range[0] + n - 1].adjust = !slots[seg.range[0] + n - 1].adjust"
              >
                {{ slots[seg.range[0] + n - 1].adjust ? "服从调剂" : "不服从" }}
              </button>

              <span class="mnz-vrow__prob" :class="strategyOf(slots[seg.range[0] + n - 1].prob).key">
                <em>录取率</em>
                {{ probabilityText(slots[seg.range[0] + n - 1].prob) }}
              </span>

              <span class="mnz-vrow__ops">
                <button type="button" title="上移" :disabled="seg.range[0] + n === 1" @click="moveSlot(seg.range[0] + n - 1, -1)">
                  <el-icon><Top /></el-icon>
                </button>
                <button type="button" title="下移" :disabled="seg.range[0] + n === TOTAL" @click="moveSlot(seg.range[0] + n - 1, 1)">
                  <el-icon><Bottom /></el-icon>
                </button>
                <button type="button" title="删除" class="is-del" @click="removeSlot(seg.range[0] + n - 1)">
                  <el-icon><Delete /></el-icon>
                </button>
              </span>
            </template>

            <template v-else>
              <el-select
                :model-value="null"
                class="mnz-vrow__school"
                placeholder="选择院校"
                filterable
                @update:model-value="slots[seg.range[0] + n - 1] = { schoolId: $event, majorNames: [], adjust: true, prob: null }; setSchool(slots[seg.range[0] + n - 1], $event)"
              >
                <el-option v-for="s in schoolOptions" :key="s.id" :value="s.id" :label="s.name">
                  <span class="mnz-vrow__school-name">{{ s.name }}</span>
                  <span class="mnz-vrow__school-meta">{{ s.province ? s.province + " · " : "" }}{{ s.is985 ? "985" : (s.is211 || s.isDoubleFirstClass) ? "双一流" : s.province ? "" : "推荐投放" }}</span>
                </el-option>
              </el-select>
              <span class="mnz-vrow__empty">选择院校后可配置专业、服从调剂</span>
            </template>
          </div>
        </section>
      </template>

      <!-- 已保存方案 -->
      <section v-if="savedSheets.length" class="mnz-vsheets">
        <h5 class="mnz-vsheets__title">我的填报方案（{{ savedSheets.length }}）</h5>
        <div class="mnz-vsheets__grid">
          <div v-for="sheet in savedSheets" :key="sheet.id" class="mnz-vsheet-card">
            <div class="mnz-vsheet-card__body">
              <strong>{{ sheet.name }}</strong>
              <span>{{ sheet.profile.province }} · {{ sheet.profile.score }}分 · {{ sheet.slots }} 个志愿</span>
            </div>
            <div class="mnz-vsheet-card__ops">
              <button type="button" @click="loadSheet(sheet)">载入</button>
              <button type="button" class="is-del" @click="removeSheet(sheet.id)">删除</button>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
