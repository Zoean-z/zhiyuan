<script setup>
import { ArrowLeft } from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { isReady, profile, rank, score, subjectType, syncFromAuth } from "../utils/examProfile";
import { isExtremelyLowProbability, probabilityDisplayValue } from "../utils/recommendation";

/**
 * 院校详情页：数据源改为后端 /api/universities/{id}（真实数据库，
 * 含概率拆解 + 近三年录取 + 开设专业），不再使用本地 mock。
 */

const route = useRoute();
const router = useRouter();

const detailData = ref(null);
const loading = ref(false);
const loadError = ref("");
const majorIdByName = ref({});

async function loadMajorMap() {
  try {
    const data = await (await fetch("/api/majors")).json();
    const map = {};
    for (const m of data.majors || []) map[m.name] = m.id;
    majorIdByName.value = map;
  } catch (e) {
    console.error("加载专业目录失败", e);
  }
}

function majorLink(major) {
  const id = majorIdByName.value[major.name];
  return `/majors/${id || encodeURIComponent(major.name)}`;
}

async function fetchDetail() {
  const id = route.params.id;
  loading.value = true;
  try {
    const params = new URLSearchParams({
      examProvince: profile.province,
      subjectType: subjectType.value
    });
    if (score.value != null) params.set("score", String(score.value));
    if (rank.value != null) params.set("userRank", String(rank.value));
    const resp = await fetch(`/api/universities/${id}?${params.toString()}`);
    if (!resp.ok) throw new Error("HTTP " + resp.status);
    detailData.value = await resp.json();
  } catch (ex) {
    loadError.value = String(ex?.message || ex);
    detailData.value = null;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  syncFromAuth();
  fetchDetail();
  loadMajorMap();
});

watch([() => route.params.id, score, rank, subjectType, () => profile.province], fetchDetail);

const school = computed(() => detailData.value);
const probability = computed(() => detailData.value?.probability || null);
const detail = computed(() => probability.value);
const history = computed(() =>
  (detailData.value?.cutoffHistory || []).map((h) => ({
    year: h.admissionYear,
    score: h.cutoffScore,
    minRank: h.minRank,
    source: "backend"
  }))
);
const majors = computed(() =>
  (detailData.value?.majors || []).map((m, i) => ({
    code: String(m.majorName || `m${i}`),
    name: m.majorName || "—",
    cutoffScore: m.cutoffScore,
    minRank: m.minRank,
    planCount: m.planCount,
    durationYears: m.durationYears,
    tuitionPerYear: m.tuitionPerYear
  }))
);

const TABS = ["院校概况", "近三年录取", "开设专业", "招生计划"];
const tab = ref("院校概况");

const levelTags = computed(() => {
  const s = detailData.value;
  if (!s) return [];
  return [s.is985 ? "985" : "", s.is211 ? "211" : "", s.isDoubleFirstClass ? "双一流" : ""].filter(Boolean);
});

const extremelyLow = computed(() => isExtremelyLowProbability(detail.value));
const strategy = computed(() => {
  if (extremelyLow.value) return { key: "unknown", label: "概率极低", full: "概率极低" };
  if (!detail.value || detail.value.probability == null || !detail.value.strategy) return null;
  const key = String(detail.value.strategy).toUpperCase();
  const view = {
    RUSH: { key: "rush", label: "冲", full: "冲刺" },
    SAFE: { key: "safe", label: "稳", full: "稳妥" },
    GUARANTEE: { key: "guard", label: "保", full: "保底" }
  }[key];
  if (!view) return null;
  return { ...view, label: detail.value.strategyLabel || view.label };
});
const displayProbability = computed(() => {
  const value = probabilityDisplayValue(detail.value);
  return value == null ? null : String(value);
});

const planRows = computed(() => majors.value.slice(0, 8));

function goProfile() {
  router.push({ path: "/volunteer" });
}

function goFill() {
  router.push({ path: "/volunteer", query: { school: school.value?.id } });
}

function fmt(value) {
  return value == null ? "—" : Number(value).toLocaleString();
}

function rankGapLine(detailValue) {
  if (detailValue?.rankGap == null) return "位次待测";
  return detailValue.rankGap >= 0 ? "位次靠前" : "位次落后";
}

function scoreGapLine(detailValue) {
  if (detailValue?.scoreGap == null) return "分数待测";
  return detailValue.scoreGap >= 0 ? "分数高出" : "分数低于";
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="查大学" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <button class="gkd-back" type="button" @click="router.push('/schools')">
            <el-icon><ArrowLeft /></el-icon>
            返回查大学
          </button>

          <template v-if="school">
            <!-- 院校头部 -->
            <header class="gkd-hero">
              <GkSchoolLogo :school="school" size="page" />
              <div class="gkd-hero__info">
                <h1>
                  {{ school.name }}
                  <span class="gkd-hero__loc">{{ school.province }}</span>
                </h1>
                <p class="gkd-hero__meta">
                  {{ school.schoolType || "综合类" }} · {{ school.nature || "公办" }} · {{ levelTags.join(" / ") || "普通院校" }}
                  <template v-if="school.tier"> · {{ school.tier }}</template>
                  <template v-if="school.softRanking"> · 软科排名 #{{ school.softRanking }}</template>
                  <template v-if="school.postgraduateRate"> · 保研率 {{ school.postgraduateRate }}%</template>
                </p>
                <p class="gkd-hero__tags">
                  <i v-for="item in levelTags" :key="item" class="is-level">{{ item }}</i>
                  <i v-for="item in (school.schoolTags || [])" :key="item">{{ item }}</i>
                </p>
              </div>

              <!-- 录取概率卡：分数来自全局考生档案（登录时确定、志愿填报页可改） -->
              <aside class="gkd-prob" :class="strategy ? `is-${strategy.key}` : 'is-empty'">
                <template v-if="strategy">
                  <p class="gkd-prob__label">我的录取概率</p>
                  <strong>{{ displayProbability }}<i>%</i></strong>
                  <span class="gkd-prob__seg">{{ strategy.full }}</span>
                  <ul class="gkd-prob__why">
                    <li>
                      我的位次 <b>{{ fmt(rank) }}</b> · 院校最低位次 <b>{{ fmt(detail?.minRank) }}</b>
                    </li>
                    <li>
                      {{ rankGapLine(detail) }}
                      <b>{{ detail.rankGap == null ? "待测" : fmt(Math.abs(detail.rankGap)) }}</b> 名（权重 75%）
                    </li>
                    <li>
                      {{ scoreGapLine(detail) }}
                      <b>{{ detail.scoreGap == null ? "待测" : Math.abs(detail.scoreGap) }}</b> 分（权重 25%）
                    </li>
                    <li v-if="extremelyLow">
                      {{ detail.explanation || "差距超出模型可测算区间，概率极低" }}
                    </li>
                  </ul>
                </template>
                <template v-else>
                  <p class="gkd-prob__label">我的录取概率</p>
                  <strong class="gkd-prob__placeholder">待测算</strong>
                  <span class="gkd-prob__seg">{{ profile.province }} · {{ profile.firstSubject }}类 · 未设置分数</span>
                  <p class="gkd-prob__hint">分数在登录时确定，可在志愿填报页的高考信息中修改，全站同步生效</p>
                  <button class="gkd-prob__cta" type="button" @click="goProfile">去设置高考信息</button>
                </template>
                <button v-if="strategy && !extremelyLow" class="gkd-prob__cta" type="button" @click="goFill">加入志愿填报</button>
              </aside>
            </header>

            <!-- 关键数据条 -->
            <ul class="gkd-kpis">
              <li>
                <span>{{ detail?.admissionYear || "近年" }} 最低分</span>
                <strong>{{ detail?.cutoffScore ?? "—" }}</strong>
              </li>
              <li>
                <span>{{ detail?.admissionYear || "近年" }} 最低位次</span>
                <strong>{{ fmt(detail?.minRank) }}</strong>
              </li>
              <li>
                <span>开设专业</span>
                <strong>{{ majors.length }} <i>个</i></strong>
              </li>
            </ul>

            <!-- Tab -->
            <div class="gkd-tabs">
              <button
                v-for="item in TABS"
                :key="item"
                type="button"
                :class="{ 'is-active': tab === item }"
                @click="tab = item"
              >
                {{ item }}
              </button>
            </div>

            <!-- 院校概况 -->
            <section v-if="tab === '院校概况'" class="gkd-card">
              <h3>院校概况</h3>
              <dl class="gkd-facts">
                <div><dt>所在地</dt><dd>{{ school.province }}</dd></div>
                <div><dt>院校类型</dt><dd>{{ school.schoolType || "综合类" }}</dd></div>
                <div><dt>办学性质</dt><dd>{{ school.nature || "公办" }}</dd></div>
                <div><dt>院校层次</dt><dd>{{ levelTags.join(" / ") || "普通院校" }}</dd></div>
                <div><dt>软科排名</dt><dd>{{ school.softRanking ? `#${school.softRanking}` : "—" }}</dd></div>
                <div><dt>保研率</dt><dd>{{ school.postgraduateRate ? `${school.postgraduateRate}%` : "—" }}</dd></div>
                <div><dt>研究生院</dt><dd>{{ school.hasGraduateSchool ? "有" : "无" }}</dd></div>
                <div><dt>博士点</dt><dd>{{ school.hasDoctorProgram ? "有" : "无" }}</dd></div>
                <div><dt>招生计划</dt><dd>{{ school.planCount ? `${school.planCount} 人` : "—" }}</dd></div>
                <div><dt>院校特色</dt><dd>{{ (school.schoolTags || []).join(" / ") || "—" }}</dd></div>
              </dl>
              <p class="gkd-note">
                数据说明：录取线来自比赛验证数据，概率按位次差 75% + 分差 25% 加权，与
                <code>RecommendationPolicyService</code> 同一套口径）。概率是参考不是保证，受招生计划、报考热度与专业组差异影响。
              </p>
            </section>

            <!-- 近三年录取 -->
            <section v-else-if="tab === '近三年录取'" class="gkd-card">
              <h3>近三年录取数据（{{ profile.province }} · {{ profile.firstSubject }}类）</h3>
              <table class="gkd-table">
                <thead>
                  <tr>
                    <th>年份</th>
                    <th>最低分</th>
                    <th>最低位次</th>
                    <th>与我的分数对比</th>
                    <th>与我的位次对比</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in history" :key="row.year">
                    <td>{{ row.year }} <i class="gkd-tag-real">比赛验证</i></td>
                    <td><b>{{ row.score }}</b></td>
                    <td>{{ fmt(row.minRank) }}</td>
                    <td>
                      <span v-if="isReady" :class="score - row.score >= 0 ? 'gkd-up' : 'gkd-down'">
                        {{ score - row.score >= 0 ? "+" : "" }}{{ score - row.score }} 分
                      </span>
                      <span v-else class="gkd-muted">未填分数</span>
                    </td>
                    <td>
                      <span v-if="rank != null && row.minRank != null" :class="row.minRank - rank >= 0 ? 'gkd-up' : 'gkd-down'">
                        {{ row.minRank - rank >= 0 ? "靠前" : "落后" }} {{ fmt(Math.abs(row.minRank - rank)) }} 名
                      </span>
                      <span v-else-if="isReady" class="gkd-muted">位次待测</span>
                      <span v-else class="gkd-muted">未填分数</span>
                    </td>
                  </tr>
                  <tr v-if="!history.length">
                    <td colspan="5" class="gkd-muted">暂无该省录取线数据</td>
                  </tr>
                </tbody>
              </table>
              <p class="gkd-note">
                数据来源为后端比赛验证录取线；位次用于辅助比较，不代表最终录取结果。
              </p>
            </section>

            <!-- 开设专业 -->
            <section v-else-if="tab === '开设专业'" class="gkd-card">
              <h3>开设专业（{{ majors.length }} 个）</h3>
              <ul class="gkd-majors">
                <li v-for="major in majors" :key="major.code" @click="router.push(majorLink(major))">
                  <span class="gkd-majors__name">{{ major.name }}</span>
                  <span class="gkd-majors__score">
                    参考分 <b>{{ major.cutoffScore ?? "—" }}</b>（位次 {{ fmt(major.minRank) }}）
                    <template v-if="major.planCount"> · 计划 {{ major.planCount }} 人</template>
                    <template v-if="major.durationYears"> · {{ major.durationYears }} 年</template>
                    <template v-if="major.tuitionPerYear"> · ¥{{ major.tuitionPerYear }}/年</template>
                  </span>
                  <span class="gkd-majors__more">专业详情 &gt;</span>
                </li>
                <li v-if="!majors.length" class="gkd-muted">暂无开设专业数据</li>
              </ul>
            </section>

            <!-- 招生计划 -->
            <section v-else class="gkd-card">
              <h3>{{ profile.province }} 招生参考（{{ profile.firstSubject }}类 · {{ profile.batch }}）</h3>
              <table class="gkd-table">
                <thead>
                  <tr>
                    <th>专业</th>
                    <th>参考最低分</th>
                    <th>参考最低位次</th>
                    <th>计划数</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in planRows" :key="row.code">
                    <td>{{ row.name }}</td>
                    <td><b>{{ row.cutoffScore ?? "—" }}</b></td>
                    <td>{{ fmt(row.minRank) }}</td>
                    <td>{{ row.planCount ?? "—" }}</td>
                  </tr>
                  <tr v-if="!planRows.length">
                    <td colspan="4" class="gkd-muted">暂无专业数据</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </template>

          <p v-else class="gks-empty">
            {{ loading ? "加载中…" : loadError ? `加载失败：${loadError}` : "没有找到这所院校，请返回查大学重新选择" }}
          </p>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
