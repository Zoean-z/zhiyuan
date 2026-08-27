<script setup>
import { ArrowDown, Search } from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { isReady, profile, rank, score, subjectType, syncFromAuth } from "../utils/examProfile";
import { isExtremelyLowProbability, probabilityDisplayValue } from "../utils/recommendation";
import searchIcon from "../assets/gk_search_icon.png";

const router = useRouter();
const route = useRoute();

function applyHeaderQuery(value) {
  const preset = Array.isArray(value) ? value[0] : value;
  if (typeof preset === "string") keyword.value = preset.trim();
}

onMounted(() => {
  syncFromAuth();
  applyHeaderQuery(route.query.q);
  fetchSchools();
  loadMajors();
});
watch(() => route.query.q, applyHeaderQuery);

/* 数据源：后端 /api/universities（公开接口，无需登录），分页拉全量后本地筛选 */
const schools = ref([]);
const loading = ref(false);
const loadError = ref("");

async function fetchSchools() {
  if (loading.value) return;
  loading.value = true;
  try {
    const params = new URLSearchParams({
      examProvince: profile.province,
      subjectType: subjectType.value,
      withDataOnly: "true",
      size: "100"
    });
    if (score.value != null) params.set("score", String(score.value));
    if (rank.value != null) params.set("userRank", String(rank.value));
    params.set("page", "1");
    const first = await (await fetch(`/api/universities?${params.toString()}`)).json();
    const total = Number(first.total || 0);
    const pages = Math.max(1, Math.ceil(total / 100));
    const all = [...(first.items || [])];
    for (let p = 2; p <= Math.min(pages, 15); p++) {
      params.set("page", String(p));
      const pageData = await (await fetch(`/api/universities?${params.toString()}`)).json();
      all.push(...(pageData.items || []));
    }
    schools.value = all;
  } catch (ex) {
    loadError.value = String(ex?.message || ex);
  } finally {
    loading.value = false;
  }
}

const PROVINCE_OPTS = computed(() => ["不限", ...new Set(schools.value.map((s) => s.province).filter(Boolean))]);
const TYPE_OPTS = computed(() => ["不限", ...new Set(schools.value.map((s) => s.schoolType).filter(Boolean))]);
const NATURE_OPTS = ["不限", "公办", "民办", "中外合作办学"];
const FEATURE_OPTS = ["不限", "985", "双一流"];
const SORT_OPTS = ["默认排序", "录取概率由高到低", "分数由高到低", "分数由低到高"];

/* 专业筛选：真实专业目录（/api/majors）+ 开设院校集合（/api/majors/{id}/schools） */
const majors = ref([]);
const majorSchoolIds = ref(null);
const MAJOR_OPTS = computed(() => ["不限", ...majors.value.map((m) => m.name)]);

const provinceFilter = ref("不限");
const typeFilter = ref("不限");
const natureFilter = ref("不限");
const featureFilter = ref("不限");
const majorFilter = ref("不限");
const sortKey = ref("默认排序");
const keyword = ref("");

async function loadMajors() {
  try {
    const data = await (await fetch("/api/majors")).json();
    majors.value = data.majors || [];
  } catch (e) {
    console.error("加载专业目录失败", e);
  }
}

async function applyMajor(name) {
  majorFilter.value = name;
  if (name === "不限") {
    majorSchoolIds.value = null;
    return;
  }
  const major = majors.value.find((m) => m.name === name);
  if (!major) {
    majorSchoolIds.value = null;
    return;
  }
  try {
    const params = new URLSearchParams({
      province: profile.province,
      subjectType: subjectType.value
    });
    if (score.value != null) params.set("score", String(score.value));
    if (rank.value != null) params.set("userRank", String(rank.value));
    const list = await (await fetch(`/api/majors/${major.id}/schools?${params.toString()}`)).json();
    majorSchoolIds.value = new Set((list || []).map((s) => s.universityId));
  } catch (e) {
    majorSchoolIds.value = null;
  }
}

watch([score, rank, subjectType, () => profile.province], () => {
  fetchSchools();
  if (majorFilter.value !== "不限") applyMajor(majorFilter.value);
});

function minScoreOf(school) {
  return school.cutoffScore ?? null;
}
function probOfSchool(school) {
  return school.probability || null;
}

function badgeOf(school) {
  const detail = probOfSchool(school);
  if (isExtremelyLowProbability(detail)) {
    return { detail, key: "unknown", label: "概率极低", value: "0%", flame: false };
  }
  if (!detail || detail.probability == null || !detail.strategy) return null;
  const key = String(detail.strategy).toUpperCase();
  const view = {
    RUSH: { key: "rush", label: "冲" },
    SAFE: { key: "safe", label: "稳" },
    GUARANTEE: { key: "guard", label: "保" }
  }[key];
  if (!view) return null;
  return {
    detail,
    ...view,
    label: detail.strategyLabel || view.label,
    value: `${detail.probability}%`,
    flame: view.key === "rush"
  };
}

function probTip(school) {
  const detail = probOfSchool(school);
  if (!detail || detail.cutoffScore == null) return "暂无录取数据";
  const minRankText = detail.minRank == null ? "—" : detail.minRank.toLocaleString();
  const base = `${detail.admissionYear || "近年"} 最低分 ${detail.cutoffScore}分 / 最低位次 ${minRankText}（后端录取数据）`;
  if (detail.probability == null) {
    if (isExtremelyLowProbability(detail)) {
      return `${base}；${detail.explanation || "差距超出模型可测算区间，概率极低"}`;
    }
    return `${base}（设置高考分数后可测算录取概率）`;
  }
  const rankText = detail.rankGap == null
    ? "位次待测"
    : detail.rankGap >= 0
      ? `位次靠前 ${detail.rankGap.toLocaleString()} 名`
      : `位次落后 ${Math.abs(detail.rankGap).toLocaleString()} 名`;
  const scoreText = detail.scoreGap == null
    ? "分数待测"
    : detail.scoreGap >= 0
      ? `分数高出 ${detail.scoreGap} 分`
      : `分数低 ${Math.abs(detail.scoreGap)} 分`;
  return `${base}；你${rankText}、${scoreText} → 录取概率 ${detail.probability}%`;
}

function rankGapText(detail) {
  if (detail.rankGap == null) return "位次待测";
  return detail.rankGap >= 0
    ? `位次靠前 ${detail.rankGap.toLocaleString()} 名`
    : `位次落后 ${Math.abs(detail.rankGap).toLocaleString()} 名`;
}
const filtered = computed(() => {
  const kw = keyword.value.trim();
  const list = schools.value.filter((school) => {
    if (provinceFilter.value !== "不限" && school.province !== provinceFilter.value) return false;
    if (typeFilter.value !== "不限" && school.schoolType !== typeFilter.value) return false;
    if (natureFilter.value !== "不限" && school.nature !== natureFilter.value) return false;
    if (featureFilter.value !== "不限") {
      if (featureFilter.value === "双一流") {
        // 双一流 ≡ 211：选双一流时 211 院校同样命中（20260820 概念归并）
        if (!school.is211 && !school.isDoubleFirstClass) return false;
      } else if (featureFilter.value === "985" && !school.is985) {
        return false;
      }
    }
    if (majorSchoolIds.value && !majorSchoolIds.value.has(school.id)) return false;
    if (kw && !school.name.includes(kw)) return false;
    return true;
  });
  const sorted = [...list];
  if (sortKey.value === "分数由高到低") sorted.sort((a, b) => (minScoreOf(b) ?? -1) - (minScoreOf(a) ?? -1));
  else if (sortKey.value === "分数由低到高") sorted.sort((a, b) => (minScoreOf(a) ?? Number.MAX_SAFE_INTEGER) - (minScoreOf(b) ?? Number.MAX_SAFE_INTEGER));
  else if (sortKey.value === "录取概率由高到低" && isReady.value) {
    sorted.sort((a, b) => (probabilityDisplayValue(probOfSchool(b)) ?? -1) - (probabilityDisplayValue(probOfSchool(a)) ?? -1));
  }
  return sorted;
});

function applyType(value) {
  typeFilter.value = value;
  natureFilter.value = "不限";
  featureFilter.value = "不限";
}

/**
 * 【修复】原来点列表项会跳到 /agent（AI 对话），根本看不了院校详情，
 * 项目里也没有院校详情页。现在新增 /schools/:id 详情页，点卡片直接进详情。
 */
function openSchool(school) {
  router.push({ name: "school-detail", params: { id: school.id } });
}

/**
 * 【分数统一】本页不再提供分数输入：分数只来自全局考生档案（登录时确定，
 * 志愿填报页可修改并全站生效）。这里只负责把用户带去唯一维护入口。
 */
function goProfile() {
  router.push({ path: "/volunteer" });
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="查大学" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gks-card">
            <div class="gks-search">
              <el-dropdown trigger="click" popper-class="gks-drop">
                <button type="button" class="gks-select">
                  <span :class="{ 'is-set': provinceFilter !== '不限' }">{{ provinceFilter === "不限" ? "位置" : provinceFilter }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <div class="gks-drop__grid">
                    <button v-for="p in PROVINCE_OPTS" :key="p" type="button" :class="['gks-drop__opt', { 'is-active': provinceFilter === p }]" @click="provinceFilter = p">{{ p }}</button>
                  </div>
                </template>
              </el-dropdown>

              <el-dropdown trigger="click" popper-class="gks-drop gks-drop--wide">
                <button type="button" class="gks-select">
                  <span :class="{ 'is-set': typeFilter !== '不限' || natureFilter !== '不限' || featureFilter !== '不限' }">
                    {{ typeFilter !== "不限" || natureFilter !== "不限" || featureFilter !== "不限" ? [typeFilter, natureFilter, featureFilter].filter((v) => v !== "不限").join("/") : "类型" }}
                  </span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <div class="gks-drop__groups">
                    <div class="gks-drop__group">
                      <p class="gks-drop__label">院校类型</p>
                      <div class="gks-drop__grid">
                        <button v-for="t in TYPE_OPTS" :key="t" type="button" :class="['gks-drop__opt', { 'is-active': typeFilter === t }]" @click="applyType(t)">{{ t }}</button>
                      </div>
                    </div>
                    <div class="gks-drop__group">
                      <p class="gks-drop__label">办学性质</p>
                      <div class="gks-drop__grid">
                        <button v-for="n in NATURE_OPTS" :key="n" type="button" :class="['gks-drop__opt', { 'is-active': natureFilter === n }]" @click="natureFilter = n">{{ n }}</button>
                      </div>
                    </div>
                    <div class="gks-drop__group">
                      <p class="gks-drop__label">院校特色</p>
                      <div class="gks-drop__grid">
                        <button v-for="f in FEATURE_OPTS" :key="f" type="button" :class="['gks-drop__opt', { 'is-active': featureFilter === f }]" @click="featureFilter = f">{{ f }}</button>
                      </div>
                    </div>
                  </div>
                </template>
              </el-dropdown>

              <el-dropdown trigger="click" popper-class="gks-drop gks-drop--wide">
                <button type="button" class="gks-select">
                  <span :class="{ 'is-set': majorFilter !== '不限' }">{{ majorFilter === "不限" ? "专业" : majorFilter }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <div class="gks-drop__groups">
                    <div class="gks-drop__group">
                      <p class="gks-drop__label">按专业筛选院校（真实开设数据）</p>
                      <div class="gks-drop__grid">
                        <button v-for="mj in MAJOR_OPTS" :key="mj" type="button" :class="['gks-drop__opt', { 'is-active': majorFilter === mj }]" @click="applyMajor(mj)">{{ mj }}</button>
                      </div>
                    </div>
                  </div>
                </template>
              </el-dropdown>

              <el-dropdown trigger="click" popper-class="gks-drop gks-drop--sort">
                <button type="button" class="gks-select">
                  <span :class="{ 'is-set': sortKey !== '默认排序' }">{{ sortKey === "默认排序" ? "排序" : sortKey }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <div class="gks-drop__col">
                    <button v-for="s in SORT_OPTS" :key="s" type="button" :class="['gks-drop__opt', { 'is-active': sortKey === s }]" @click="sortKey = s">{{ s }}</button>
                  </div>
                </template>
              </el-dropdown>

              <div class="gks-searchbar">
                <img class="gks-searchbar__icon" :src="searchIcon" alt="" />
                <input v-model="keyword" type="text" placeholder="输入院校名称" @keyup.enter="keyword = keyword.trim()" />
                <button type="button" @click="keyword = keyword.trim()">
                  <el-icon><Search /></el-icon>搜索
                </button>
              </div>
            </div>

            <div class="gks-meta">
              <p class="gks-count">院校 <b>{{ filtered.length }}</b> 所</p>
              <!-- 考生档案条：全局唯一（examProfile），分数在登录/志愿填报页维护，此页只展示 -->
              <div class="gks-score" :class="{ 'is-empty': !isReady }">
                <template v-if="isReady">
                  <span class="gks-score__info">
                    {{ profile.province }} · {{ profile.firstSubject }}类 · <b>{{ score }} 分</b> · {{ rank == null ? "暂无位次数据" : `位次约 ${rank.toLocaleString()}` }}
                  </span>
                  <button class="gks-score__edit" type="button" @click="goProfile">修改</button>
                </template>
                <template v-else>
                  <span class="gks-score__info">尚未设置高考分数，无法测算录取概率</span>
                  <button class="gks-score__edit" type="button" @click="goProfile">去设置</button>
                </template>
              </div>
            </div>
          </div>

          <ul class="gks-list">
            <li v-for="school in filtered" :key="school.id" class="gks-item" @click="openSchool(school)">
              <GkSchoolLogo :school="school" size="page" />
              <div class="gks-item__info">
                <p class="gks-item__name">
                  {{ school.name }}<span class="gks-item__city">{{ school.province }}</span>
                </p>
                <p class="gks-item__core">本科 · {{ school.schoolType || "综合" }} · {{ school.nature || "公办" }}</p>
                <p class="gks-item__tags">
                  <i v-for="tag in [school.is985 ? '985' : '', school.is211 ? '211' : '', school.isDoubleFirstClass ? '双一流' : ''].filter(Boolean)" :key="tag" class="is-level">{{ tag }}</i>
                  <i v-for="tag in (school.tags ? [school.tags] : [])" :key="tag">{{ tag }}</i>
                </p>
              </div>
              <button
                v-if="badgeOf(school)"
                class="gks-prob"
                :class="`is-${badgeOf(school).key}`"
                type="button"
                :title="probTip(school)"
                @click.stop="openSchool(school)"
              >
                <span>录取概率</span>
                <b>
                  <svg
                    v-if="badgeOf(school).flame"
                    class="gks-prob__flame"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
                    <path d="M9.879 16.121A3 3 0 1012.015 11L11 14H9c0 .768.293 1.536.879 2.121z" />
                  </svg>
                  {{ badgeOf(school).label }} {{ badgeOf(school).value }}
                </b>
                <i class="gks-prob__gap">
                  {{ rankGapText(badgeOf(school).detail) }}
                </i>
              </button>
              <button v-else class="gks-prob gks-prob--empty" type="button" @click.stop="goProfile()">
                <span>录取概率</span>
                <b>设置分数后测算</b>
                <i class="gks-prob__gap">去设置高考信息 &gt;</i>
              </button>
            </li>
            <li v-if="!filtered.length" class="gks-empty">没有符合条件的院校，试试放宽筛选条件</li>
          </ul>

          <!-- 数据来源与测算方法 -->
          <details class="gks-source">
            <summary>数据来源与测算方法<i>查看当前数据口径与概率依据</i></summary>
            <div class="gks-source__body">
              <div class="gks-source__block">
                <h4>① 录取概率怎么算？</h4>
                <p>
                  先用<b>一分一段曲线</b>把你的分数换成全省位次；再把「院校最低位次 − 我的位次」（<b>权重 75%</b>）与「我的分数 − 院校最低分」（<b>权重 25%</b>）分段换算并加权求和；
                  结果按 ≥75% 保底 / 55–74% 稳妥 / 35–54% 冲刺 / &lt;35% 高风险分档。与后端 <code>RecommendationPolicyService</code> 是同一套算法，前后端不会出现两个数。
                </p>
              </div>
              <div class="gks-source__block">
                <h4>② 数据从哪里来？</h4>
                <ul>
                  <li><b>当前录取线</b>：使用已入库的比赛验证数据，院校线按同校专业最低分聚合，仅用于功能验证</li>
                  <li><b>院校排名</b>：软科 2025 中国大学排名、校友会 2026 中国大学排名（艾瑞深研究院）</li>
                  <li><b>保研率</b>：各校 2024 / 2025 届毕业生就业质量报告</li>
                  <li><b>硕博点</b>：各校研究生院一级学科硕士点 / 博士点统计</li>
                  <li><b>联盟标签</b>：C9、华东五校、中坚九校、国防七子、建筑老八校、四大工学院、电气四虎、机械五虎（社会通行口径）</li>
                  <li><b>数据覆盖</b>：只在当前省份和科类有数据时展示，不用其他省份数据补算</li>
                  <li><b>招生计划 / 专业组计划数</b>：演示值，仅供参考</li>
                </ul>
              </div>
              <div class="gks-source__block">
                <h4>③ 判断逻辑</h4>
                <p>
                  录取线直接读取后端当前省份、科类的比赛验证数据，不在前端生成或跨省换算。
                  概率反映的是「以你的位次相对该校去年门槛的历史位置」，受招生计划增减、报考热度、专业组差异影响，是参考不是保证。
                </p>
              </div>
            </div>
          </details>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
