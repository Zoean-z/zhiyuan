<script setup>
import { ArrowDown, Search } from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { SCHOOLS, schoolClubTags, schoolLoc, schoolTags } from "../utils/exploreData";
import { probDetailOf, schoolCutoff } from "../utils/volunteerCore";
import { strategyOf } from "../utils/scoreModel";
import { isReady, profile, rank, score, subjectType, syncFromAuth } from "../utils/examProfile";
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
});
watch(() => route.query.q, applyHeaderQuery);

/* 与掌上高考 /school/search 一致的筛选项（军校-国际本科快捷栏已按需求舍弃） */
const PROVINCE_OPTS = ["不限", ...Array.from(new Set(SCHOOLS.map((s) => s.province)))];
const TYPE_OPTS = ["不限", "综合", "理工", "师范", "农林", "医药", "财经", "政法", "语言", "艺术", "体育", "民族"];
const NATURE_OPTS = ["不限", "公办", "民办", "中外合作办学"];
const FEATURE_OPTS = ["不限", "985", "双一流", "强基", "研究生院"];
const SORT_OPTS = ["默认排序", "录取概率由高到低", "分数由高到低", "分数由低到高"];

/* 专业大类 → 院校类型映射（数据为院校维度，按类型近似筛选） */
const MAJOR_TYPE_MAP = {
  "计算机类": "理工", "电子信息类": "理工", "机械类": "理工", "电气类": "理工", "材料类": "理工",
  "土木类": "理工", "自动化类": "理工", "航空航天类": "理工", "法学类": "政法", "政治学类": "政法",
  "教育学类": "师范", "中国语言文学类": "语言", "外国语言文学类": "语言", "经济学类": "财经",
  "金融学类": "财经", "工商管理类": "财经", "临床医学类": "医药", "基础医学类": "医药", "药学类": "医药",
  "植物生产类": "农林", "动物医学类": "农林", "设计学类": "艺术", "音乐与舞蹈学类": "艺术", "体育学类": "体育"
};
const MAJOR_OPTS = ["不限", ...Object.keys(MAJOR_TYPE_MAP)];

const provinceFilter = ref("不限");
const typeFilter = ref("不限");
const natureFilter = ref("不限");
const featureFilter = ref("不限");
const majorFilter = ref("不限");
const sortKey = ref("默认排序");
const keyword = ref("");

const FEATURE_FIELD = { "985": "is985" };

/**
 * 【修复】原来这里的录取概率是 `42 + (school.id * 37) % 52`，
 * 完全与考生分数无关，所以「这里面的概率是啥」根本解释不了。
 * 现在：没填分数就不显示概率（引导填分数），填了就按
 * 「位次差 75% + 分差 25%」的后端同款算法算，并把依据写在 tooltip 里。
 */
const cutoffOpts = computed(() => ({
  province: profile.province,
  subjectType: subjectType.value,
  userRank: rank.value
}));

function minScoreOf(school) {
  const cutoff = schoolCutoff(school, cutoffOpts.value);
  return cutoff ? cutoff.score : null;
}
function probOfSchool(school) {
  return probDetailOf(school, score.value, cutoffOpts.value);
}

/**
 * 徽章模型：有概率 → 保/稳/冲/险四档；已设分数但位次落后超 3000 名、
 * 分差低于线超 10 分（模型的可测算下界）→ 直接判「险 <1%」，
 * 对齐 gaokao.cn「难 1%」红徽章的呈现，而不是显示成空态。
 */
function badgeOf(school) {
  const detail = probOfSchool(school);
  if (detail.probability != null) {
    const seg = strategyOf(detail.probability);
    return { detail, key: seg.key, label: seg.label, value: `${detail.probability}%`, flame: seg.key === "rush" || seg.key === "risk" };
  }
  if (isReady.value && detail.cutoff && detail.rankGap != null && detail.rankGap < 0) {
    return { detail, key: "risk", label: "险", value: "<1%", flame: true };
  }
  return null;
}

function probTip(school) {
  const detail = probOfSchool(school);
  const cutoff = detail.cutoff;
  if (!cutoff) return "暂无录取数据";
  const minRankText = cutoff.minRank == null ? "—" : cutoff.minRank.toLocaleString();
  const sourceText = cutoff.source === "real"
    ? "（浙江考试院2026一段投档线）"
    : cutoff.source === "backend"
      ? "（后端录取数据）"
      : cutoff.source === "derived"
        ? "（估算）"
        : "";
  const noteText = cutoff.note ? `【${cutoff.note}】` : "";
  const base = `${cutoff.year} 年最低分 ${cutoff.score}分 / 最低位次 ${minRankText}${sourceText}${noteText}`;
  if (detail.probability == null) {
    if (isReady.value && detail.rankGap != null && detail.rankGap < 0) {
      return `${base}；你位次落后 ${Math.abs(detail.rankGap).toLocaleString()} 名，超出可测算范围，录取概率极低（<1%）`;
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
  const list = SCHOOLS.filter((school) => {
    if (provinceFilter.value !== "不限" && school.province !== provinceFilter.value) return false;
    if (typeFilter.value !== "不限" && !school.type.startsWith(typeFilter.value)) return false;
    if (natureFilter.value !== "不限" && school.nature !== natureFilter.value) return false;
    if (featureFilter.value !== "不限") {
      if (featureFilter.value === "双一流") {
        // 双一流 ≡ 211：选双一流时 211 院校同样命中（20260820 概念归并）
        if (!school.is211 && !school.isDoubleFirstClass) return false;
      } else {
        const field = FEATURE_FIELD[featureFilter.value];
        if (field && !school[field]) return false;
      }
    }
    if (majorFilter.value !== "不限" && !school.type.startsWith(MAJOR_TYPE_MAP[majorFilter.value] || "")) return false;
    if (kw && !school.name.includes(kw)) return false;
    return true;
  });
  const sorted = [...list];
  if (sortKey.value === "分数由高到低") sorted.sort((a, b) => (minScoreOf(b) ?? -1) - (minScoreOf(a) ?? -1));
  else if (sortKey.value === "分数由低到高") sorted.sort((a, b) => (minScoreOf(a) ?? Number.MAX_SAFE_INTEGER) - (minScoreOf(b) ?? Number.MAX_SAFE_INTEGER));
  else if (sortKey.value === "录取概率由高到低" && isReady.value) {
    sorted.sort((a, b) => (probOfSchool(b).probability ?? -1) - (probOfSchool(a).probability ?? -1));
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
                      <p class="gks-drop__label">按专业大类筛选院校</p>
                      <div class="gks-drop__grid">
                        <button v-for="mj in MAJOR_OPTS" :key="mj" type="button" :class="['gks-drop__opt', { 'is-active': majorFilter === mj }]" @click="majorFilter = mj">{{ mj }}</button>
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
                    {{ profile.province }} · {{ profile.firstSubject }}类 · <b>{{ score }} 分</b> · 位次约 {{ rank.toLocaleString() }}
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
                  {{ school.name }}<span class="gks-item__city">{{ schoolLoc(school) }}</span>
                </p>
                <p class="gks-item__core">本科 · {{ school.type }} · {{ school.nature }} · {{ school.belong }}</p>
                <p class="gks-item__tags">
                  <i v-for="tag in schoolTags(school)" :key="tag" class="is-level">{{ tag }}</i>
                  <i v-for="tag in schoolClubTags(school)" :key="tag">{{ tag }}</i>
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

          <!-- 数据来源与测算方法：把「概率怎么算 / 数据从哪来 / 根据在哪」写在明面上 -->
          <details class="gks-source">
            <summary>数据来源与测算方法<i>概率怎么算的？数据是真实的吗？点开看依据</i></summary>
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
                  <li><b>浙江一分一段 / 投档线</b>：浙江省教育考试院 2025、2026 年普通类一分一段表与一段平行投档分数线（真实数据，卡片徽章 tooltip 会标注来源）</li>
                  <li><b>院校排名</b>：软科 2025 中国大学排名、校友会 2026 中国大学排名（艾瑞深研究院）</li>
                  <li><b>保研率</b>：各校 2024 / 2025 届毕业生就业质量报告</li>
                  <li><b>硕博点</b>：各校研究生院一级学科硕士点 / 博士点统计</li>
                  <li><b>联盟标签</b>：C9、华东五校、中坚九校、国防七子、建筑老八校、四大工学院、电气四虎、机械五虎（社会通行口径）</li>
                  <li><b>外省录取线</b>：以该校在浙江的真实最低位次百分位、按该省考生规模换算，属<b>估算值</b>（tooltip 标注「估算」）</li>
                  <li><b>招生计划 / 专业组计划数</b>：演示值，仅供参考</li>
                </ul>
              </div>
              <div class="gks-source__block">
                <h4>③ 判断逻辑</h4>
                <p>
                  录取线取用优先级：<b>后端数据库真实数据 → 内置官方投档线（浙江 2026）→ 真实位次百分位换算的估算值</b>；每个数字都能追溯到来源。
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
