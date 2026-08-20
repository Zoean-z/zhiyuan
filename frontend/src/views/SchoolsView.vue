<script setup>
import { ArrowDown, Search } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { SCHOOLS, schoolLoc, schoolTags } from "../utils/exploreData";
import { probDetailOf, schoolCutoff } from "../utils/volunteerCore";
import { strategyOf } from "../utils/scoreModel";
import { isReady, profile, rank, score, setScore, subjectType, syncFromAuth } from "../utils/examProfile";
import hotIcon from "../assets/gk_hot.png";
import searchIcon from "../assets/gk_search_icon.png";

const router = useRouter();

onMounted(() => syncFromAuth());

/* 与掌上高考 /school/search 一致的筛选项（军校-国际本科快捷栏已按需求舍弃） */
const PROVINCE_OPTS = ["不限", ...Array.from(new Set(SCHOOLS.map((s) => s.province)))];
const TYPE_OPTS = ["不限", "综合", "理工", "师范", "农林", "医药", "财经", "政法", "语言", "艺术", "体育", "民族"];
const NATURE_OPTS = ["不限", "公办", "民办", "中外合作办学"];
const FEATURE_OPTS = ["不限", "985", "211", "双一流", "强基", "研究生院"];
const SORT_OPTS = ["默认排序", "录取概率由高到低", "分数由高到低", "分数由低到高", "搜索热度由高到低", "保研率由高到低"];

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

const FEATURE_FIELD = { "985": "is985", "211": "is211", "双一流": "isDoubleFirstClass" };

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
  return cutoff ? cutoff.score : 0;
}
function minRankOf(school) {
  const cutoff = schoolCutoff(school, cutoffOpts.value);
  return cutoff ? cutoff.minRank : null;
}
function probOfSchool(school) {
  return probDetailOf(school, score.value, cutoffOpts.value);
}
function probTip(school) {
  const detail = probOfSchool(school);
  const cutoff = detail.cutoff;
  if (!cutoff) return "暂无录取数据";
  const minRankText = cutoff.minRank == null ? "—" : cutoff.minRank.toLocaleString();
  const base = `${cutoff.year} 年最低分 ${cutoff.score}分 / 最低位次 ${minRankText}`;
  if (detail.probability == null) return `${base}（填入分数后可测算录取概率）`;
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
function isHot(school) {
  return school.id <= 8;
}
function baoyanOf(school) {
  return 18 + ((school.id * 13) % 38);
}

const filtered = computed(() => {
  const kw = keyword.value.trim();
  const list = SCHOOLS.filter((school) => {
    if (provinceFilter.value !== "不限" && school.province !== provinceFilter.value) return false;
    if (typeFilter.value !== "不限" && !school.type.startsWith(typeFilter.value)) return false;
    if (natureFilter.value !== "不限" && school.nature !== natureFilter.value) return false;
    if (featureFilter.value !== "不限") {
      const field = FEATURE_FIELD[featureFilter.value];
      if (field && !school[field]) return false;
    }
    if (majorFilter.value !== "不限" && !school.type.startsWith(MAJOR_TYPE_MAP[majorFilter.value] || "")) return false;
    if (kw && !school.name.includes(kw)) return false;
    return true;
  });
  const sorted = [...list];
  if (sortKey.value === "分数由高到低") sorted.sort((a, b) => minScoreOf(b) - minScoreOf(a));
  else if (sortKey.value === "分数由低到高") sorted.sort((a, b) => minScoreOf(a) - minScoreOf(b));
  else if (sortKey.value === "录取概率由高到低" && isReady.value) {
    sorted.sort((a, b) => (probOfSchool(b).probability ?? -1) - (probOfSchool(a).probability ?? -1));
  }
  else if (sortKey.value === "搜索热度由高到低") sorted.sort((a, b) => a.id - b.id);
  else if (sortKey.value === "保研率由高到低") sorted.sort((a, b) => baoyanOf(b) - baoyanOf(a));
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

function onScoreInput(event) {
  setScore(event.target.value);
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
              <!-- 考生分数条：全站共享（examProfile），在哪一页填都算，换页不丢 -->
              <div class="gks-score">
                <span>{{ profile.province }} · {{ profile.firstSubject }}类</span>
                <input
                  type="number"
                  min="100"
                  max="750"
                  placeholder="我的分数"
                  :value="profile.score ?? ''"
                  @input="onScoreInput"
                />
                <b v-if="isReady">位次约 {{ rank.toLocaleString() }}</b>
                <em v-else>填分数即可测录取概率</em>
              </div>
            </div>
          </div>

          <ul class="gks-list">
            <li v-for="school in filtered" :key="school.id" class="gks-item" @click="openSchool(school)">
              <GkSchoolLogo :school="school" size="page" />
              <div class="gks-item__info">
                <p class="gks-item__name">
                  {{ school.name }}<span class="gks-item__city">{{ schoolLoc(school) }}</span>
                  <img v-if="isHot(school)" class="gks-item__hot" :src="hotIcon" alt="热门" />
                </p>
                <p class="gks-item__core">本科 · {{ school.type }} · {{ school.nature }}</p>
                <p class="gks-item__tags">
                  <i v-for="tag in schoolTags(school)" :key="tag">{{ tag }}</i>
                </p>
                <p class="gks-item__line">
                  参考最低分 <b>{{ minScoreOf(school) }}</b>
                  <em v-if="minRankOf(school)">最低位次 {{ minRankOf(school).toLocaleString() }}</em>
                </p>
              </div>
              <button
                v-if="isReady && probOfSchool(school).probability != null"
                class="gks-prob"
                type="button"
                :title="probTip(school)"
                @click.stop="openSchool(school)"
              >
                <span>录取概率</span>
                <b :class="`is-${strategyOf(probOfSchool(school).probability).key}`">
                  {{ strategyOf(probOfSchool(school).probability).label }}{{ probOfSchool(school).probability }}%
                </b>
                <i class="gks-prob__gap">
                  {{ rankGapText(probOfSchool(school)) }}
                </i>
              </button>
              <button v-else class="gks-prob gks-prob--empty" type="button" @click.stop="openSchool(school)">
                <span>录取概率</span>
                <b>填分数测算</b>
                <i class="gks-prob__gap">查看详情 &gt;</i>
              </button>
            </li>
            <li v-if="!filtered.length" class="gks-empty">没有符合条件的院校，试试放宽筛选条件</li>
          </ul>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
