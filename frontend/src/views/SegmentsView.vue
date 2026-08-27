<script setup>
import { ElMessage } from "element-plus";
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import {
  PROVINCES,
  profile,
  score as sharedScore,
  subjectType as sharedSubjectType,
  syncFromAuth
} from "../utils/examProfile";
import { fetchRankLookup, fetchScoreRankCurve } from "../utils/scoreRankApi";

const router = useRouter();
const SUBJECT_OPTIONS = [
  { value: "PHYSICS", label: "物理类" },
  { value: "HISTORY", label: "历史类" }
];

const province = ref(profile.province);
const subjectType = ref(sharedSubjectType.value);
const mappingYear = ref(null);
const scoreInput = ref("");
const rows = ref([]);
const loading = ref(false);
const loadError = ref("");
const queryLoading = ref(false);
const queryResult = ref(null);
let initialized = false;
let curveRequestVersion = 0;
let rankRequestVersion = 0;

const subjectLabel = computed(() =>
  SUBJECT_OPTIONS.find((item) => item.value === subjectType.value)?.label || ""
);

async function loadCurve() {
  const requestVersion = ++curveRequestVersion;
  loading.value = true;
  loadError.value = "";
  queryResult.value = null;
  try {
    const data = await fetchScoreRankCurve(province.value, subjectType.value);
    if (requestVersion !== curveRequestVersion) return;
    rows.value = data.points;
    mappingYear.value = data.mappingYear;
  } catch (error) {
    if (requestVersion !== curveRequestVersion) return;
    rows.value = [];
    mappingYear.value = null;
    loadError.value = String(error?.message || "一分一段数据加载失败");
  } finally {
    if (requestVersion === curveRequestVersion) loading.value = false;
  }
}

const chart = computed(() => {
  const list = rows.value.filter((row) => row.segmentCount != null);
  if (!list.length) return { points: "", area: "", max: 0, scores: [] };
  const width = 640;
  const height = 200;
  const max = Math.max(...list.map((row) => row.segmentCount)) || 1;
  const stepX = width / Math.max(1, list.length - 1);
  const points = list
    .map((row, index) => `${(index * stepX).toFixed(1)},${(height - (row.segmentCount / max) * (height - 12) - 6).toFixed(1)}`)
    .join(" ");
  const scores = [0, 0.25, 0.5, 0.75, 1].map((ratio) => {
    const index = Math.min(list.length - 1, Math.round((list.length - 1) * ratio));
    return list[index]?.score ?? "—";
  });
  return { points, area: `0,${height} ${points} ${width},${height}`, max, scores };
});

async function queryScore() {
  const value = Number(scoreInput.value);
  if (!Number.isFinite(value) || value < 0 || value > 750) {
    queryResult.value = null;
    ElMessage.warning("请输入 0–750 之间的有效分数");
    return;
  }

  const requestVersion = ++rankRequestVersion;
  queryLoading.value = true;
  try {
    const result = await fetchRankLookup(province.value, subjectType.value, Math.round(value));
    if (requestVersion !== rankRequestVersion) return;
    const exactPoint = rows.value.find((row) => row.score === result.score);
    queryResult.value = { ...result, segmentCount: exactPoint?.segmentCount ?? null };
  } catch (error) {
    if (requestVersion !== rankRequestVersion) return;
    queryResult.value = null;
    ElMessage.error(String(error?.message || "位次查询失败"));
  } finally {
    if (requestVersion === rankRequestVersion) queryLoading.value = false;
  }
}

function askProbability() {
  if (queryResult.value?.rank == null) return;
  router.push({
    path: "/agent",
    query: {
      q: `${province.value}${subjectLabel.value}${queryResult.value.score}分，位次约${queryResult.value.rank}名，能上哪些大学？`
    }
  });
}

onMounted(async () => {
  syncFromAuth();
  province.value = profile.province;
  subjectType.value = sharedSubjectType.value;
  scoreInput.value = sharedScore.value ?? "";
  initialized = true;
  await loadCurve();
});

watch([province, subjectType], () => {
  if (initialized) void loadCurve();
});
</script>

<template>
  <div class="gk-page">
    <GkHeader active="一分一段" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gk-filter">
            <div class="gk-filter__row">
              <span class="gk-filter__label">省份</span>
              <button
                v-for="item in PROVINCES"
                :key="item"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': province === item }"
                @click="province = item"
              >
                {{ item }}
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">科类</span>
              <button
                v-for="item in SUBJECT_OPTIONS"
                :key="item.value"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': subjectType === item.value }"
                @click="subjectType = item.value"
              >
                {{ item.label }}
              </button>
              <span class="gk-filter__label gk-filter__label--gap">年份</span>
              <button type="button" class="gk-filter__opt is-active">{{ mappingYear || "—" }}</button>
            </div>
            <div class="gk-filter__row gk-filter__row--search">
              <span class="gk-filter__label">定位</span>
              <div class="gk-filter__search">
                <input v-model="scoreInput" type="text" placeholder="请输入您的分数" @keyup.enter="queryScore" />
                <button type="button" :disabled="queryLoading" @click="queryScore">{{ queryLoading ? "查询中" : "查询" }}</button>
              </div>
              <button class="gk-filter__ghost-btn" type="button" :disabled="queryResult?.rank == null" @click="askProbability">测录取概率</button>
            </div>
          </div>

          <div v-if="queryResult" class="gk-seg-result">
            <template v-if="queryResult.rank != null">
              <b>{{ queryResult.score }}</b> 分：
              <template v-if="queryResult.segmentCount != null">本段 <b>{{ queryResult.segmentCount }}</b> 人，</template>
              位次约 <b>{{ queryResult.rank.toLocaleString() }}</b> 名
            </template>
            <template v-else><b>{{ queryResult.score }}</b> 分：暂无位次数据</template>
            <button v-if="queryResult.rank != null" type="button" @click="askProbability">让 AI 推荐院校 &gt;</button>
          </div>

          <div v-if="loading" class="gk-empty">正在加载</div>
          <div v-else-if="loadError" class="gk-empty">{{ loadError }}</div>
          <div v-else-if="!rows.length" class="gk-empty">当前省份和科类暂无一分一段数据</div>
          <template v-else>
            <div v-if="chart.points" class="gk-seg-chart">
              <div class="gk-seg-chart__head">
                <h4>{{ province }}{{ subjectLabel }} · 分数分布</h4>
                <span>人数峰值 {{ chart.max }} 人 / 分</span>
              </div>
              <svg viewBox="0 0 640 200" preserveAspectRatio="none" class="gk-seg-chart__svg">
                <polygon :points="chart.area" fill="rgba(255,102,0,0.10)" />
                <polyline :points="chart.points" fill="none" stroke="#FF6600" stroke-width="2.5" stroke-linejoin="round" />
              </svg>
              <div class="gk-seg-chart__axis">
                <span v-for="axisScore in chart.scores" :key="axisScore">{{ axisScore }}分</span>
              </div>
            </div>

            <div class="gk-htable">
              <div class="gk-htable__row gk-htable__row--head">
                <span class="gk-htable__cell">分数</span>
                <span class="gk-htable__cell">本段人数</span>
                <span class="gk-htable__cell">累计人数</span>
              </div>
              <div
                v-for="row in rows"
                :key="row.score"
                class="gk-htable__row"
                :class="{ 'is-alt': row.score % 2 === 1, 'is-hit': queryResult && row.score === queryResult.score }"
              >
                <span class="gk-htable__cell gk-htable__cell--score">{{ row.score }}</span>
                <span class="gk-htable__cell gk-htable__cell--num">{{ row.segmentCount ?? "—" }}</span>
                <span class="gk-htable__cell gk-htable__cell--num">{{ row.rankValue.toLocaleString() }}</span>
              </div>
            </div>
          </template>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
