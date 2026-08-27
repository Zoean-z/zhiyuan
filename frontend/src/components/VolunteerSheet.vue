<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Bottom, Top, Promotion } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import GkSchoolLogo from "./GkSchoolLogo.vue";
import {
  SEGMENTS, TOTAL, normalizeSchoolLike, readCurrentSheet, segmentOfIndex, writeCurrentSheet
} from "../utils/volunteerCore";
import { normalizeSubjectType } from "../utils/scoreModel";

const props = defineProps({
  profile: { type: Object, required: true },
  initialTab: { type: String, default: "pick" },
  initialView: { type: String, default: "detail" }
});
const router = useRouter();

/* ===== 模块切换：模拟填报（选校） / 志愿表（参考 mnzy.gaokao.cn 双模块） ===== */
const activeTab = ref(props.initialTab === "sheet" ? "sheet" : "pick");

/* ===== 院校库：后端 /api/universities（80 所精选大学 + 录取线/概率） ===== */
const schools = ref([]);
const loadingSchools = ref(false);
/* 专业下拉选项：后端专业目录（88 个专业） */
const majorOptions = ref([]);
/* 每所大学开设专业（按需加载：展开"可填专业"时拉详情） */
const majorCache = ref({});

async function fetchSchools() {
  if (loadingSchools.value) return;
  loadingSchools.value = true;
  try {
    const params = new URLSearchParams({
      examProvince: props.profile.province,
      subjectType: normalizeSubjectType(props.profile.firstSubject || props.profile.subjects?.[0] || props.profile.subjectType),
      withDataOnly: "true",
      size: "100"
    });
    /* 关键：把考生分数/位次传给后端 → 每所大学算出真实概率 → 冲/稳/保计数有数据 */
    if (props.profile.score != null && Number(props.profile.score) > 0) {
      params.set("score", String(Number(props.profile.score)));
    }
    if (myRank.value != null && Number(myRank.value) > 0) {
      params.set("userRank", String(Number(myRank.value)));
    }
    const resp = await fetch(`/api/universities?${params.toString()}`);
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const data = await resp.json();
    schools.value = data.items || [];
  } catch (ex) {
    console.error("加载院校库失败", ex);
  } finally {
    loadingSchools.value = false;
  }
  try {
    const data = await (await fetch("/api/majors")).json();
    majorOptions.value = (data.majors || []).map((m) => m.name);
  } catch (ex) {
    console.error("加载专业目录失败", ex);
  }
}

/* 展开"可填专业"时按需加载该校专业列表（详情接口含 majors） */
async function loadSchoolMajors(id) {
  if (majorCache.value[id]) return;
  try {
    const params = new URLSearchParams({
      examProvince: props.profile.province,
      subjectType: normalizeSubjectType(props.profile.firstSubject || props.profile.subjects?.[0] || props.profile.subjectType)
    });
    if (props.profile.score != null && Number(props.profile.score) > 0) {
      params.set("score", String(Number(props.profile.score)));
    }
    if (myRank.value != null && Number(myRank.value) > 0) {
      params.set("userRank", String(Number(myRank.value)));
    }
    const resp = await fetch(`/api/universities/${id}?${params.toString()}`);
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const d = await resp.json();
    majorCache.value = { ...majorCache.value, [id]: (d.majors || []).map((m) => m.majorName).filter(Boolean) };
  } catch (ex) {
    console.error("加载专业列表失败", ex);
    majorCache.value = { ...majorCache.value, [id]: [] };
  }
}

onMounted(fetchSchools);

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
  slots.value.forEach((s, index) => {
    if (!s) return;
    const key = segmentOfIndex(index).key;
    if (key in stat) stat[key] += 1;
    else stat.unknown += 1;
  });
  return stat;
});

/* 位次只接受共享档案已经从后端解析出的值，不在组件内换算。 */
const myRank = computed(() => {
  const value = Number(props.profile.rank);
  return Number.isFinite(value) && value > 0 ? value : null;
});

/* ===== 模拟填报：院校卡片数据（后端真实录取线/概率） ===== */
const pickStrategy = ref("all");
const pickType = ref("全部");
const sortBy = ref("概率");
const pickQuery = ref("");
const lineBounds = computed(() => {
  const lines = schools.value.map((s) => s.cutoffScore).filter((v) => v != null);
  if (!lines.length) return null;
  return [Math.min(...lines), Math.max(...lines)];
});
const scoreRange = ref([0, 0]);
watch(lineBounds, (b) => {
  scoreRange.value = b ? [...b] : [0, 0];
}, { immediate: true });

function backendStrategy(probability) {
  const key = String(probability?.strategy || "").toUpperCase();
  const view = {
    RUSH: { key: "rush", label: "冲刺" },
    SAFE: { key: "safe", label: "稳妥" },
    GUARANTEE: { key: "guard", label: "保底" }
  }[key];
  return view ? { ...view, label: probability.strategyLabel || view.label } : { key: "unknown", label: "待测" };
}

function schoolFacts(school) {
  const prob = school.probability?.probability ?? null;
  const strategy = backendStrategy(school.probability);
  const majorCount = school.majorCount ?? 0;
  return {
    school,
    prob,
    strategy,
    line: school.cutoffScore ?? null,
    majorList: majorCache.value[school.id] || [],
    majorCount
  };
}

const pickTypes = computed(() => ["全部", ...new Set(schools.value.map((s) => s.schoolType).filter(Boolean))]);

const filteredFacts = computed(() => {
  let list = schools.value.map(schoolFacts).filter((f) => f.line != null && (!lineBounds.value || (f.line >= scoreRange.value[0] && f.line <= scoreRange.value[1])));
  if (pickType.value !== "全部") list = list.filter((f) => f.school.schoolType === pickType.value);
  const q = pickQuery.value.trim();
  if (q) list = list.filter((f) => f.school.name.includes(q) || (f.school.province || "").includes(q));
  const byKey = { all: null, rush: "rush", safe: "safe", guard: "guard" };
  if (byKey[pickStrategy.value]) list = list.filter((f) => f.strategy.key === byKey[pickStrategy.value]);
  if (sortBy.value === "概率") list = [...list].sort((a, b) => compareProbability(a.prob, b.prob));
  else list = [...list].sort((a, b) => (a.school.id ?? 0) - (b.school.id ?? 0));
  return list;
});
const pickCounts = computed(() => {
  const list = schools.value.map(schoolFacts).filter((f) => f.line != null && (!lineBounds.value || (f.line >= scoreRange.value[0] && f.line <= scoreRange.value[1])));
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
  /* 展开时按需加载该校专业列表 */
  if (expandedId.value === id) loadSchoolMajors(id);
}
function inSheet(facts) {
  return slots.value.some((s) => s && s.schoolId === facts.school.id);
}
function addFromPick(facts) {
  const segKey = facts.strategy.key;
  if (segKey === "unknown") {
    ElMessage.warning("该院校暂无后端冲稳保结论，不能自动分配志愿段");
    return;
  }
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
    prob: facts.prob,
    minRank: facts.school.minRank ?? null,
    schoolSource: "backend",
    majorSource: facts.majorList.length ? "backend" : null,
    probabilitySource: facts.prob == null ? null : "backend",
    dataSource: "backend"
  };
  ElMessage.success(`已加入第 ${target + 1} 志愿位（${seg.label}）`);
}
function askAbout(facts) {
  router.push({ path: "/agent", query: { q: `帮我分析${facts.school.name}：我现在${props.profile.score}分，后端返回${facts.strategy.label}，参考概率${probabilityText(facts.prob)}，${facts.school.admissionYear || "当前"}年最低分为${facts.line ?? "暂无"}。请说明数据局限，并分析是否适合放进志愿表。` } });
}

/* 下拉可选项 = 后端院校库 + 已填的外部院校（来自推荐结果投放） */
const schoolOptions = computed(() => {
  const extras = [];
  slots.value.forEach((s) => {
    if (!s || s.schoolId == null) return;
    if (schools.value.some((x) => x.id === s.schoolId)) return;
    if (extras.some((x) => x.id === s.schoolId)) return;
    extras.push({
      id: s.schoolId,
      name: s.schoolName || `院校 ${s.schoolId}`,
      province: "",
      schoolType: "综合类",
      nature: "",
      is985: false,
      is211: false,
      isDoubleFirstClass: false
    });
  });
  return [...schools.value, ...extras];
});

function probOfHere(school) {
  return school.probability?.probability ?? null;
}

function setSchool(slot, schoolId) {
  if (!schoolId) {
    slot.schoolId = null;
    slot.schoolName = "";
    slot.majorNames = [];
    slot.prob = null;
    slot.minRank = null;
    slot.schoolSource = null;
    slot.majorSource = null;
    slot.probabilitySource = null;
    slot.dataSource = null;
    return;
  }
  const school = schoolOptions.value.find((s) => s.id === schoolId) || null;
  const backendSchool = school ? schools.value.find((item) => item.id === school.id) || null : null;
  slot.schoolId = school ? school.id : null;
  slot.schoolName = school ? school.name : "";
  slot.majorNames = [];
  slot.prob = backendSchool ? probOfHere(backendSchool) : null;
  slot.minRank = backendSchool?.minRank ?? null;
  slot.schoolSource = backendSchool ? "backend" : null;
  slot.majorSource = null;
  slot.probabilitySource = slot.prob == null ? null : "backend";
  slot.dataSource = backendSchool ? "backend" : null;
}

function schoolOf(slot) {
  if (!slot) return null;
  const matched = schools.value.find((s) => s.id === slot.schoolId);
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
    s ? [i + 1, s.schoolName, probabilityText(s.prob), segmentOfIndex(i).label, (s.majorNames || []).join("、"), s.adjust ? "是" : "否"] : null
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

/* （风险诊断模块已按要求移除：内容晦涩、用户体验差） */

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
    ElMessage.warning("志愿表为空，请先手动选择院校后再保存");
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

/* profile 分数变化时，院校库概率同步刷新（后端已按分数/位次算好） */
watch(
  () => [props.profile.province, props.profile.firstSubject, props.profile.subjectType, props.profile.score, props.profile.rank],
  () => {
    majorCache.value = {};
    fetchSchools();
  }
);

watch(() => props.profile.score, () => {
  slots.value.forEach((s) => {
    if (!s?.schoolId) return;
    const matched = schools.value.find((x) => x.id === s.schoolId);
    if (matched) {
      s.prob = probOfHere(matched);
      s.minRank = matched.minRank ?? null;
      s.schoolSource = "backend";
      s.probabilitySource = s.prob == null ? null : "backend";
      s.dataSource = "backend";
    }
  });
});

defineExpose({ smartSort });
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
        <div v-if="lineBounds" class="mnz-pick__score">
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
          <button v-for="t in pickTypes" :key="t" type="button" :class="{ 'is-active': pickType === t }" @click="pickType = t">
            {{ t }}
          </button>
        </div>
        <div class="mnz-pick__seg">
          <el-tooltip content="按概率：把录取概率高的院校排前面，方便优先选择把握大的；按院校：按院校编号排序浏览" placement="top">
            <button v-for="s in ['概率', '院校']" :key="s" type="button" :class="{ 'is-active': sortBy === s }" @click="sortBy = s">
            {{ s === '概率' ? '按概率' : '按院校' }}
            </button>
          </el-tooltip>
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
              <span class="mnz-pcard__disc">{{ f.disciplines }}</span>
            </div>

            <div class="mnz-pcard__tags">
              <span v-for="t in [f.school.province, f.school.schoolType, f.school.nature, f.school.is985 ? '985' : '', (f.school.is211 || f.school.isDoubleFirstClass) ? '双一流' : ''].filter(Boolean)" :key="t" class="mnz-pcard__tag">{{ t }}</span>
            </div>

            <div class="mnz-pcard__years">
              <div class="mnz-pcard__year">
                <header>{{ f.school.admissionYear || "当前" }}年{{ f.school.dataKind === "SIMULATED" ? "比赛验证数据" : "录取数据" }}</header>
                <p><label>最低分</label><b>{{ f.line }} 分</b></p>
                <p><label>最低位次</label><b>{{ f.school.minRank == null ? "暂无" : `${f.school.minRank.toLocaleString("zh-CN")} 名` }}</b></p>
                <p><label>来源说明</label><span>{{ f.school.calibrationSource || "后端暂未提供" }}</span></p>
              </div>
            </div>

            <div class="mnz-pcard__foot">
              <button type="button" class="mnz-pcard__majors-btn" @click="toggleExpand(f.school.id)">
                可填专业({{ f.majorCount ?? f.majorList.length }})
                <el-icon><Promotion /></el-icon>
              </button>
              <button type="button" class="mnz-pcard__ask" @click="askAbout(f)">问小智解读</button>
              <div v-if="expandedId === f.school.id" class="mnz-pcard__major-list">
                <template v-if="f.majorList.length">
                  <span v-for="m in f.majorList" :key="m">{{ m }}</span>
                </template>
                <span v-else class="mnz-pcard__major-loading">专业列表加载中…</span>
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
          <button type="button" class="mnz-vsheet__op" @click="smartSort">智能排序</button>
          <button type="button" class="mnz-vsheet__op" @click="exportSheet">导出</button>
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
                <span class="mnz-table__prob" :class="segmentOfIndex(r.idx - 1).key"><em>录取率</em> {{ probabilityText(r.slot.prob) }}</span>
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
              <td colspan="6" class="mnz-table__empty">暂未添加志愿，可切换「详细模式」逐位填报</td>
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
                <el-option v-for="m in majorOptions" :key="m" :label="m" :value="m" />
              </el-select>

              <button
                type="button"
                class="mnz-vrow__adjust"
                :class="{ 'is-on': slots[seg.range[0] + n - 1].adjust }"
                @click="slots[seg.range[0] + n - 1].adjust = !slots[seg.range[0] + n - 1].adjust"
              >
                {{ slots[seg.range[0] + n - 1].adjust ? "服从调剂" : "不服从" }}
              </button>

              <span class="mnz-vrow__prob" :class="seg.key">
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
