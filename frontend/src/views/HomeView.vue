<script setup>
import { Calendar, Collection, DataLine, EditPen, MagicStick, OfficeBuilding, Promotion, Trophy } from "@element-plus/icons-vue";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import { MAJORS } from "../utils/exploreData";
import { NEWS_TAGS, newsById } from "../utils/newsData";
import { strategyOf } from "../utils/scoreModel";
import {
  ENTRANT_TYPES,
  SECOND_SUBJECTS,
  confirmProfile,
  isReady,
  percent,
  profile,
  rank,
  score,
  setFirstSubject,
  setScore,
  subjectType,
  syncFromAuth,
  toggleSecondSubject
} from "../utils/examProfile";

const router = useRouter();

/* ── 轮播 Banner ──
 * 【修复】原先每张 slide 的主/副按钮全部指向 /agent，两个按钮点下去效果一模一样。
 * 现在主按钮 = 功能页（填报/匹配），副按钮 = 另一个不同入口，同一张卡不会重复。 */
const SLIDES = [
  {
    kicker: "2026 届智能报考季",
    title: "我向往的大学",
    desc: "AI 智能填报 · 一分都不浪费，基于分数、位次与选科生成「冲稳保」涨度志愿方案",
    primaryText: "开始智能填报",
    primaryTo: { path: "/volunteer" },
    ghostText: "问小智",
    ghostTo: { path: "/agent" }
  },
  {
    kicker: "模拟报志愿",
    title: "冲稳保 · 三档智能定位",
    desc: "输入分数与选科，即刻测算可冲击、较稳妥、可保底的院校数量与位次区间",
    primaryText: "智能选大学",
    primaryTo: { path: "/choose" },
    ghostText: "看一分一段",
    ghostTo: { path: "/segments" }
  },
  {
    kicker: "AI 报考助手在线",
    title: "问小智 · 有问必答",
    desc: "分数能上哪些大学？专业怎么选？志愿怎么填？AI 在线答疑，随时帮你分析",
    primaryText: "立即提问",
    primaryTo: { path: "/agent" },
    ghostText: "查院校排行",
    ghostTo: { path: "/rank" }
  }
];
const slideIndex = ref(0);
let slideTimer = null;

/* 数据源：后端 /api/universities（公开接口），首页学校数据全部来自真实数据库 */
const schools = ref([]);
const loading = ref(false);

async function fetchSchools() {
  if (loading.value) return;
  loading.value = true;
  try {
    const scoreVal = score.value == null ? "" : score.value;
    const rankVal = rank.value == null ? "" : rank.value;
    const base = `/api/universities?size=100&score=${scoreVal}&userRank=${rankVal}`;
    const first = await (await fetch(base + "&page=1")).json();
    const total = Number(first.total || 0);
    const pages = Math.max(1, Math.ceil(total / 100));
    const all = [...(first.items || [])];
    for (let p = 2; p <= Math.min(pages, 15); p++) {
      const pageData = await (await fetch(base + `&page=${p}`)).json();
      all.push(...(pageData.items || []));
    }
    schools.value = all;
  } catch (ex) {
    // 拉取失败时保持空列表，不阻塞首页
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  syncFromAuth();
  fetchSchools();
  slideTimer = window.setInterval(() => {
    slideIndex.value = (slideIndex.value + 1) % SLIDES.length;
  }, 5000);
});
onBeforeUnmount(() => window.clearInterval(slideTimer));

/* ── 模拟报志愿面板 ──
 * 【修复】右上角「普通类/艺术类」原来是两个 <span>，根本点不了；现在是真按钮。
 * 【修复】面板里填的分数/选科以前只存在本页 ref，一跳转就丢；
 *          现在写入 examProfile（全站共享），志愿填报、查大学、智能选大学都能直接用。
 * 【修复】位次不再用本页自己的假公式，统一走 scoreModel。 */
const scoreTip = ref(false);

function onScoreInput(event) {
  setScore(event.target.value);
  if (score.value != null) scoreTip.value = false;
}

const simStats = computed(() => {
  if (!isReady.value) return null;
  const buckets = { rush: 0, safe: 0, guard: 0 };
  schools.value.forEach((school) => {
    const detail = school.probability;
    if (!detail || detail.probability == null) return;
    const key = strategyOf(detail.probability).key;
    if (buckets[key] != null) buckets[key] += 1;
  });
  return {
    score: score.value,
    rank: rank.value,
    rankLow: Math.round(rank.value * 0.985),
    rankHigh: Math.round(rank.value * 1.015),
    pct: percent.value,
    ...buckets
  };
});

/** 智能推荐大学：带着刚填的分数/选科进「智能选大学」
 *  【修复】原来点这个按钮跳 /recommend（需登录的旧推荐页，未登录直接被踢到登录页，
 *  管理员账号还会被路由守卫强制踢到 /admin），所以看起来就是「跳到一个废弃界面」。 */
function goSmartMatch() {
  if (!isReady.value) {
    scoreTip.value = true;
    return;
  }
  confirmProfile();
  router.push({
    path: "/choose",
    query: {
      score: score.value,
      rank: rank.value,
      subject: profile.firstSubject,
      second: (profile.secondSubjects || []).join(","),
      province: profile.province
    }
  });
}

function goFillSheet() {
  if (isReady.value) confirmProfile();
  router.push({ path: "/volunteer" });
}

/* ── 快捷入口（全站唯一的功能入口区） ──
 * 【修复】原首页有三套重复入口：快捷入口(7) + 快捷卡片 TILES(4) + 报考专题(9)，
 * 指向的页面大量重叠。现在只保留这一组。 */
const QUICK_ENTRIES = [
  { label: "查大学", to: "/schools", icon: OfficeBuilding, color: "#3b82f6", bg: "#e8f1fe" },
  { label: "查专业", to: "/majors", icon: Collection, color: "#10b981", bg: "#e6f8f1" },
  { label: "志愿填报", to: "/volunteer", icon: EditPen, color: "#ff6600", bg: "#fff0e5" },
  { label: "智能选大学", to: "/choose", icon: MagicStick, color: "#8b5cf6", bg: "#f1ecfe" },
  { label: "院校排行", to: "/rank", icon: Trophy, color: "#f59e0b", bg: "#fdf3e0" },
  { label: "一分一段", to: "/segments", icon: DataLine, color: "#06b6d4", bg: "#e3f8fb" },
  { label: "招生计划", to: "/enroll", icon: Calendar, color: "#ec4899", bg: "#fdeaf4" }
];

/* ── 热门院校 ── */
const SCHOOL_TABS = computed(() => ["全部", ...new Set(schools.value.map((s) => s.tags).filter(Boolean))]);
const schoolTab = ref("全部");
const schoolOffset = ref(0);
const hotSchools = computed(() => {
  const list = schoolTab.value === "全部" ? schools.value : schools.value.filter((s) => (s.tags || "").includes(schoolTab.value));
  const take = Math.min(8, list.length);
  const out = [];
  for (let i = 0; i < take; i += 1) {
    out.push(list[(schoolOffset.value + i) % list.length]);
  }
  return out;
});
watch(schoolTab, () => {
  schoolOffset.value = 0;
});

function shuffleSchools() {
  schoolOffset.value += 8;
}

/* ── 热门专业 ── */
const MAJOR_LEVEL_TABS = ["本科", "专科"];
const majorLevelTab = ref("本科");
const CATEGORY_COLORS = {
  工学: ["#3b82f6", "#e8f1fe"],
  理学: ["#06b6d4", "#e3f8fb"],
  医学: ["#10b981", "#e6f8f1"],
  文学: ["#ec4899", "#fdeaf4"],
  经济学: ["#f59e0b", "#fdf3e0"],
  法学: ["#ff6600", "#fff0e5"],
  管理学: ["#8b5cf6", "#f1ecfe"],
  default: ["#6b7280", "#f3f4f6"]
};
const hotMajors = computed(() => {
  const level = majorLevelTab.value === "本科" ? 0 : 1;
  return [...MAJORS]
    .filter((m) => m.level === level)
    .sort((a, b) => (a.hot || 999) - (b.hot || 999))
    .slice(0, 5);
});

/* ── 院校热度 ── */
const heatList = computed(() =>
  schools.value.slice(0, 5).map((school, i) => ({
    name: school.name,
    id: school.id,
    hot: (9.9 - i * 0.86).toFixed(1)
  }))
);

/* ── 高考资讯（全站只保留这一处，侧边栏与首页「热点资讯」已删） ──
 * 真实资讯源：中国教育在线 gaokao.eol.cn，站内详情页 */
const NEWS_ROW_IDS = [2764667, 2764248, 2764252, 2763565, 2763535, 2762894, 2762544, 2762535];
const NEWS_ROWS = NEWS_ROW_IDS.map((id) => newsById(id)).filter(Boolean);
const NEWS_TABS = NEWS_TAGS;
const newsTab = ref("全部");
const newsList = computed(() => {
  if (newsTab.value === "全部") return NEWS_ROWS.slice(0, 7);
  return NEWS_ROWS.filter((item) => item.tag === newsTab.value);
});
const newsHeadline = computed(() => newsList.value[0] || null);
const newsRest = computed(() => newsList.value.slice(1));
</script>

<template>
  <div class="gk-page">
    <GkHeader active="首页" />

    <main class="gk-home__container gk-hp">
      <!-- ① 轮播 Banner + 模拟报志愿 -->
      <section class="gk-hp__hero">
        <div class="gk-hp__banner">
          <div
            v-for="(slide, i) in SLIDES"
            :key="slide.title"
            class="gk-hp__slide"
            :class="{ 'is-active': i === slideIndex }"
          >
            <p class="gk-hp__slide-kicker">{{ slide.kicker }}</p>
            <h2 class="gk-hp__slide-title">{{ slide.title }}</h2>
            <p class="gk-hp__slide-desc">{{ slide.desc }}</p>
            <div class="gk-hp__slide-actions">
              <button class="gk-hp__slide-primary" type="button" @click="router.push(slide.primaryTo)">{{ slide.primaryText }}</button>
              <button class="gk-hp__slide-ghost" type="button" @click="router.push(slide.ghostTo)">
                <el-icon><Promotion /></el-icon>
                {{ slide.ghostText }}
              </button>
            </div>
          </div>
          <div class="gk-hp__slide-dots">
            <button
              v-for="(slide, i) in SLIDES"
              :key="`dot-${slide.title}`"
              type="button"
              :class="{ 'is-active': i === slideIndex }"
              :aria-label="`第${i + 1}张`"
              @click="slideIndex = i"
            />
          </div>
        </div>

        <aside class="gk-hp__sim">
          <header class="gk-hp__sim-head">
            <h3>模拟报志愿</h3>
            <div class="gk-hp__sim-types">
              <button
                v-for="item in ENTRANT_TYPES"
                :key="item.key"
                type="button"
                :class="{ 'is-active': profile.entrantType === item.key }"
                @click="profile.entrantType = item.key"
              >
                {{ item.label }}
              </button>
            </div>
          </header>

          <div class="gk-hp__sim-field">
            <span class="gk-hp__sim-label">首选科目</span>
            <div class="gk-hp__sim-opts">
              <button type="button" :class="{ 'is-active': profile.firstSubject === '物理' }" @click="setFirstSubject('物理')">物理</button>
              <button type="button" :class="{ 'is-active': profile.firstSubject === '历史' }" @click="setFirstSubject('历史')">历史</button>
            </div>
          </div>

          <div class="gk-hp__sim-field">
            <span class="gk-hp__sim-label">再选科目 <em>（最多 2 门）</em></span>
            <div class="gk-hp__sim-opts gk-hp__sim-opts--four">
              <button
                v-for="item in SECOND_SUBJECTS"
                :key="item"
                type="button"
                :class="{ 'is-active': profile.secondSubjects.includes(item) }"
                @click="toggleSecondSubject(item)"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div class="gk-hp__sim-field">
            <span class="gk-hp__sim-label">分数</span>
            <input
              class="gk-hp__sim-input"
              :class="{ 'is-error': scoreTip }"
              type="number"
              min="100"
              max="750"
              placeholder="输入高考分数"
              :value="profile.score ?? ''"
              @input="onScoreInput"
            />
          </div>

          <div v-if="simStats" class="gk-hp__sim-chart">
            <div class="gk-hp__sim-chart-top">
              <strong>{{ simStats.score }}<i>分</i></strong>
              <span>位次约 {{ simStats.rankLow.toLocaleString() }}–{{ simStats.rankHigh.toLocaleString() }} 名</span>
            </div>
            <div class="gk-hp__sim-bar">
              <div :style="{ width: `${simStats.pct}%` }" />
            </div>
            <p>{{ profile.province }}{{ profile.firstSubject }}类 · 超过本省 {{ simStats.pct }}% 考生</p>
          </div>
          <div v-else class="gk-hp__sim-chart gk-hp__sim-chart--empty">
            <p>{{ scoreTip ? "请先输入 100–750 之间的高考分数" : "输入分数后，自动测算位次区间与冲稳保院校数量" }}</p>
          </div>

          <div class="gk-hp__sim-stats">
            <div><strong>{{ simStats ? simStats.rush : "—" }}</strong><span>可冲击</span></div>
            <div><strong>{{ simStats ? simStats.safe : "—" }}</strong><span>较稳妥</span></div>
            <div><strong>{{ simStats ? simStats.guard : "—" }}</strong><span>可保底</span></div>
          </div>

          <button class="gk-hp__sim-cta" type="button" @click="goSmartMatch">智能推荐大学</button>
          <button class="gk-hp__sim-sub" type="button" @click="goFillSheet">直接去填志愿表 &gt;</button>
        </aside>
      </section>

      <!-- ② 快捷入口 -->
      <nav class="gk-hp__quick" aria-label="快捷入口">
        <button v-for="entry in QUICK_ENTRIES" :key="entry.label" type="button" @click="router.push(entry.to)">
          <span class="gk-hp__quick-icon" :style="{ color: entry.color, background: entry.bg }">
            <el-icon><component :is="entry.icon" /></el-icon>
          </span>
          <span>{{ entry.label }}</span>
        </button>
      </nav>

      <!-- ③ 热门院校 -->
      <section class="gk-hp__card gk-hp__schools">
        <header class="gk-hp__card-head">
          <h3>热门院校</h3>
          <div class="gk-hp__head-right">
            <div class="gk-hp__seg">
              <button
                v-for="tab in SCHOOL_TABS"
                :key="tab"
                type="button"
                :class="{ 'is-active': schoolTab === tab }"
                @click="schoolTab = tab"
              >
                {{ tab }}
              </button>
            </div>
            <button class="gk-hp__shuffle" type="button" @click="shuffleSchools">
              <el-icon><Promotion /></el-icon>
              换一换
            </button>
          </div>
        </header>
        <div class="gk-hp__school-grid">
          <button
            v-for="school in hotSchools"
            :key="`${school.id}-${school.name}`"
            type="button"
            class="gk-hp__school"
            @click="router.push(`/schools/${school.id}`)"
          >
            <GkSchoolLogo :school="school" />
            <span class="gk-hp__school-copy">
              <strong>{{ school.name }}</strong>
              <em>{{ school.tags || "—" }}</em>
            </span>
            <span class="gk-hp__school-more">院校详情 &gt;</span>
          </button>
        </div>
      </section>

      <!-- ④ 热门专业 + 院校热度 -->
      <section class="gk-hp__row">
        <div class="gk-hp__card">
          <header class="gk-hp__card-head">
            <h3>热门专业</h3>
            <div class="gk-hp__head-right">
              <div class="gk-hp__seg">
                <button
                  v-for="tab in MAJOR_LEVEL_TABS"
                  :key="tab"
                  type="button"
                  :class="{ 'is-active': majorLevelTab === tab }"
                  @click="majorLevelTab = tab"
                >
                  {{ tab }}
                </button>
              </div>
              <span class="gk-hp__more" @click="router.push('/majors')">更多 &gt;</span>
            </div>
          </header>
          <ul class="gk-hp__majors">
            <li v-for="major in hotMajors" :key="major.code">
              <button type="button" @click="router.push(`/majors/${major.code}`)">
                <span
                  class="gk-hp__major-badge"
                  :style="{ color: (CATEGORY_COLORS[major.category] || CATEGORY_COLORS.default)[0], background: (CATEGORY_COLORS[major.category] || CATEGORY_COLORS.default)[1] }"
                >
                  {{ major.name.slice(0, 1) }}
                </span>
                <span class="gk-hp__major-copy">
                  <strong>{{ major.name }}</strong>
                  <em>{{ major.category }} · {{ major.duration }} · 毕业年薪 {{ major.salary }}</em>
                </span>
                <span class="gk-hp__major-count">{{ major.schoolCount }} 所院校开设</span>
              </button>
            </li>
          </ul>
        </div>

        <aside class="gk-hp__card gk-hp__heat">
          <header class="gk-hp__card-head">
            <h3>院校热度</h3>
            <span class="gk-hp__more" @click="router.push('/rank')">更多 &gt;</span>
          </header>
          <ol class="gk-hp__heat-list">
            <li v-for="(item, i) in heatList" :key="item.name" @click="item.id && router.push(`/schools/${item.id}`)">
              <span class="gk-hp__heat-no" :class="`gk-hp__heat-no--${i < 3 ? 'top' : 'rest'}`">{{ i + 1 }}</span>
              <GkSchoolLogo v-if="item.id" :school="{ id: item.id, name: item.name }" size="mini" />
              <span class="gk-hp__heat-name">{{ item.name }}</span>
              <span class="gk-hp__heat-val"><i /><b>{{ item.hot }}w</b></span>
            </li>
          </ol>
        </aside>
      </section>

      <!-- ⑤ 高考资讯（全站唯一一处） -->
      <section class="gk-hp__card gk-hp__news gk-hp__row--last">
        <header class="gk-hp__card-head">
          <h3>高考资讯</h3>
          <span class="gk-hp__more" @click="router.push('/news')">更多 &gt;</span>
        </header>
        <div class="gk-hp__news-tabs">
          <button
            v-for="tab in NEWS_TABS"
            :key="tab"
            type="button"
            :class="{ 'is-active': newsTab === tab }"
            @click="newsTab = tab"
          >
            {{ tab }}
          </button>
        </div>
        <div class="gk-hp__news-body">
          <router-link
            v-if="newsHeadline"
            class="gk-hp__news-headline"
            :to="`/news/${newsHeadline.id}`"
            :title="newsHeadline.title"
          >
            <em>{{ newsHeadline.tag }}</em>
            <strong>{{ newsHeadline.title }}</strong>
            <span>{{ newsHeadline.source }} · {{ newsHeadline.date }}</span>
          </router-link>
          <ul class="gk-hp__news-feed">
            <li v-for="item in newsRest" :key="item.id">
              <router-link :to="`/news/${item.id}`" :title="item.title">
                <em>{{ item.tag }}</em>
                <span class="gk-hp__feed-title">{{ item.title }}</span>
                <span class="gk-hp__feed-date">{{ item.date }}</span>
              </router-link>
            </li>
            <li v-if="!newsRest.length" class="gk-hp__news-empty">该分类暂无更多资讯</li>
          </ul>
        </div>
      </section>
    </main>

    <footer class="gk-hp__footer">
      <div class="gk-home__container">
        <p>智愿AI报考平台 · 2026 智能报考季 · 数据仅供志愿填报参考</p>
      </div>
    </footer>
  </div>
</template>
