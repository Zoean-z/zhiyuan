<script setup>
import { ElMessage } from "element-plus";
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { SEGMENT_PROVINCES, SEGMENT_SUBJECTS, buildSegments } from "../utils/exploreData";

const router = useRouter();
const province = ref("浙江");
const subject = ref("物理类");
const year = ref("2026");
const scoreInput = ref("");
const queriedScore = ref(null);

const rows = computed(() => buildSegments(province.value, subject.value));

const chart = computed(() => {
  const list = rows.value;
  if (!list.length) return { points: "", area: "", max: 0 };
  const w = 640;
  const h = 200;
  const max = Math.max(...list.map((r) => r.count)) || 1;
  const stepX = w / Math.max(1, list.length - 1);
  const points = list
    .map((r, i) => `${(i * stepX).toFixed(1)},${(h - (r.count / max) * (h - 12) - 6).toFixed(1)}`)
    .join(" ");
  const area = `0,${h} ${points} ${w},${h}`;
  return { points, area, max, w, h };
});

const queryResult = computed(() => {
  if (queriedScore.value == null) return null;
  const score = queriedScore.value;
  const exact = rows.value.find((r) => r.score === score);
  const nearest = exact || [...rows.value].sort((a, b) => Math.abs(a.score - score) - Math.abs(b.score - score))[0];
  return nearest ? { score, ...nearest, exact: Boolean(exact) } : null;
});

function queryScore() {
  const value = Number(scoreInput.value);
  if (!Number.isFinite(value) || value < 100 || value > 750) {
    queriedScore.value = null;
    ElMessage.warning("请输入 100–750 之间的有效分数");
    return;
  }
  queriedScore.value = Math.round(value);
}

function askProbability() {
  const result = queryResult.value;
  if (!result) return;
  router.push({
    path: "/agent",
    query: { q: `${province.value}${subject.value}${result.score}分，位次约${result.total}名，能上哪些大学？` }
  });
}
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
                v-for="p in SEGMENT_PROVINCES"
                :key="p"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': province === p }"
                @click="province = p"
              >
                {{ p }}
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">科类</span>
              <button
                v-for="s in SEGMENT_SUBJECTS"
                :key="s"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': subject === s }"
                @click="subject = s"
              >
                {{ s }}
              </button>
              <span class="gk-filter__label gk-filter__label--gap">年份</span>
              <button type="button" class="gk-filter__opt is-active">2026</button>
            </div>
            <div class="gk-filter__row gk-filter__row--search">
              <span class="gk-filter__label">定位</span>
              <div class="gk-filter__search">
                <input v-model="scoreInput" type="text" placeholder="请输入您的分数" @keyup.enter="queryScore" />
                <button type="button" @click="queryScore">查询</button>
              </div>
              <button class="gk-filter__ghost-btn" type="button" :disabled="!queryResult" @click="askProbability">测录取概率</button>
            </div>
          </div>

          <div v-if="queryResult" class="gk-seg-result">
            <template v-if="queryResult.exact">
              <b>{{ queryResult.score }}</b> 分：本段 <b>{{ queryResult.count }}</b> 人，最低位次约 <b>{{ queryResult.total }}</b> 名
            </template>
            <template v-else>
              <b>{{ queryResult.score }}</b> 分：最接近分数段 <b>{{ queryResult.score }}</b>（{{ province }}{{ subject }}），累计约 <b>{{ queryResult.total }}</b> 名
            </template>
            <button type="button" @click="askProbability">让 AI 推荐院校 &gt;</button>
          </div>

          <div class="gk-seg-chart">
            <div class="gk-seg-chart__head">
              <h4>{{ province }}{{ subject }} · 分数分布</h4>
              <span>人数峰值 {{ chart.max }} 人 / 分</span>
            </div>
            <svg viewBox="0 0 640 200" preserveAspectRatio="none" class="gk-seg-chart__svg">
              <polygon :points="chart.area" fill="rgba(255,102,0,0.10)" />
              <polyline :points="chart.points" fill="none" stroke="#FF6600" stroke-width="2.5" stroke-linejoin="round" />
            </svg>
            <div class="gk-seg-chart__axis">
              <span>700分</span><span>650</span><span>600</span><span>550</span><span>500分</span>
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
              <span class="gk-htable__cell gk-htable__cell--num">{{ row.count }}</span>
              <span class="gk-htable__cell gk-htable__cell--num">{{ row.total.toLocaleString() }}</span>
            </div>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
