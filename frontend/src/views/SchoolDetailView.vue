<script setup>
import { ArrowLeft } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { SCHOOLS, schoolLoc, schoolTags } from "../utils/exploreData";
import { cutoffHistory, majorCutoff, majorDetailsOfSchool, probDetailOf, schoolCutoff } from "../utils/volunteerCore";
import { strategyOf } from "../utils/scoreModel";
import { isReady, profile, rank, score, setScore, subjectType, syncFromAuth } from "../utils/examProfile";

/**
 * 院校详情页（新增）
 * 【解决的问题】Word 文档最后一条：「这里面的概率是啥…也看不了院校详情」。
 * 原项目根本没有院校详情页，查大学列表点进去是 AI 对话页。
 * 现在：/schools/:id → 概况 / 近三年录取 / 开设专业 / 招生计划 四个 tab，
 * 并把「概率怎么算出来的」完整展示（位次差 / 分差 / 权重）。
 */

const route = useRoute();
const router = useRouter();

onMounted(() => syncFromAuth());

const school = computed(() => {
  const id = Number(route.params.id);
  return SCHOOLS.find((item) => item.id === id) || null;
});

const TABS = ["院校概况", "近三年录取", "开设专业", "招生计划"];
const tab = ref("院校概况");

const opts = computed(() => ({
  province: profile.province,
  subjectType: subjectType.value,
  userRank: rank.value
}));

const cutoff = computed(() => (school.value ? schoolCutoff(school.value, opts.value) : null));
const history = computed(() => (school.value ? cutoffHistory(school.value, opts.value, 3) : []));
const detail = computed(() => (school.value ? probDetailOf(school.value, score.value, opts.value) : null));
const strategy = computed(() => (detail.value?.probability == null ? null : strategyOf(detail.value.probability)));
const majors = computed(() => (school.value ? majorDetailsOfSchool(school.value) : []));

const planRows = computed(() => {
  if (!school.value) return [];
  return majors.value.slice(0, 8).map((major, index) => {
    const mc = majorCutoff(school.value, major, opts.value);
    return {
      name: major.name,
      code: major.code,
      duration: major.duration,
      count: Math.max(2, Math.round(school.value.planCount / (majors.value.length + 4)) + ((index * 3) % 5)),
      tuition: 4600 + ((index * 7 + school.value.id) % 6) * 700,
      score: mc?.score ?? null,
      minRank: mc?.minRank ?? null
    };
  });
});

function onScoreInput(event) {
  setScore(event.target.value);
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
                  <span class="gkd-hero__loc">{{ schoolLoc(school) }}</span>
                </h1>
                <p class="gkd-hero__meta">{{ school.type }} · {{ school.nature }} · {{ school.belong }}</p>
                <p class="gkd-hero__tags">
                  <i v-for="item in schoolTags(school)" :key="item">{{ item }}</i>
                </p>
              </div>

              <!-- 录取概率卡：没分数就引导填，有分数就把依据全部展开 -->
              <aside class="gkd-prob" :class="strategy ? `is-${strategy.key}` : 'is-empty'">
                <template v-if="strategy">
                  <p class="gkd-prob__label">我的录取概率</p>
                  <strong>{{ detail.probability }}<i>%</i></strong>
                  <span class="gkd-prob__seg">{{ strategy.full }}</span>
                  <ul class="gkd-prob__why">
                    <li>
                      我的位次 <b>{{ fmt(rank) }}</b> · 院校最低位次 <b>{{ fmt(cutoff?.minRank) }}</b>
                    </li>
                    <li>
                      {{ rankGapLine(detail) }}
                      <b>{{ detail.rankGap == null ? "待测" : fmt(Math.abs(detail.rankGap)) }}</b> 名（权重 75%）
                    </li>
                    <li>
                      {{ scoreGapLine(detail) }}
                      <b>{{ detail.scoreGap == null ? "待测" : Math.abs(detail.scoreGap) }}</b> 分（权重 25%）
                    </li>
                  </ul>
                </template>
                <template v-else>
                  <p class="gkd-prob__label">填入分数测录取概率</p>
                  <input
                    class="gkd-prob__input"
                    type="number"
                    min="100"
                    max="750"
                    placeholder="我的高考分数"
                    :value="profile.score ?? ''"
                    @input="onScoreInput"
                  />
                  <span class="gkd-prob__hint">{{ profile.province }} · {{ profile.firstSubject }}类</span>
                </template>
                <button class="gkd-prob__cta" type="button" @click="goFill">加入志愿填报</button>
              </aside>
            </header>

            <!-- 关键数据条 -->
            <ul class="gkd-kpis">
              <li>
                <span>{{ cutoff?.year }} 年最低分</span>
                <strong>{{ cutoff?.score ?? "—" }}</strong>
              </li>
              <li>
                <span>{{ cutoff?.year }} 年最低位次</span>
                <strong>{{ fmt(cutoff?.minRank) }}</strong>
              </li>
              <li>
                <span>招生计划</span>
                <strong>{{ school.planCount }} <i>人</i></strong>
              </li>
              <li>
                <span>开设专业</span>
                <strong>{{ school.majorCount }} <i>个</i></strong>
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
                <div><dt>所在地</dt><dd>{{ schoolLoc(school) }}</dd></div>
                <div><dt>院校类型</dt><dd>{{ school.type }}</dd></div>
                <div><dt>办学性质</dt><dd>{{ school.nature }}</dd></div>
                <div><dt>主管部门</dt><dd>{{ school.belong }}</dd></div>
                <div><dt>院校特色</dt><dd>{{ schoolTags(school).join(" / ") || "—" }}</dd></div>
                <div><dt>今年计划变化</dt><dd>{{ school.planDelta >= 0 ? `+${school.planDelta}` : school.planDelta }} 人</dd></div>
              </dl>
              <p class="gkd-note">
                数据说明：未登录 / 未联调时，最低分与位次由本地统一模型（院校实力 → 省内位次锚点 → 一分一段曲线反算分数）生成；
                登录后调用后端 <code>/api/recommendations</code> 时，优先使用数据库里的真实录取线。
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
                    <td>{{ row.year }}</td>
                    <td><b>{{ row.score }}</b></td>
                    <td>{{ fmt(row.minRank) }}</td>
                    <td>
                      <span v-if="isReady" :class="score - row.score >= 0 ? 'gkd-up' : 'gkd-down'">
                        {{ score - row.score >= 0 ? "+" : "" }}{{ score - row.score }} 分
                      </span>
                      <span v-else class="gkd-muted">未填分数</span>
                    </td>
                    <td>
                      <span v-if="isReady && row.minRank != null" :class="row.minRank - rank >= 0 ? 'gkd-up' : 'gkd-down'">
                        {{ row.minRank - rank >= 0 ? "靠前" : "落后" }} {{ fmt(Math.abs(row.minRank - rank)) }} 名
                      </span>
                      <span v-else-if="isReady" class="gkd-muted">
                        位次待测
                      </span>
                      <span v-else class="gkd-muted">未填分数</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <p class="gkd-note">位次比分数更可靠：各年题目难度不同，分数会潮起潮落，但位次直接反映你在全省的相对位置。</p>
            </section>

            <!-- 开设专业 -->
            <section v-else-if="tab === '开设专业'" class="gkd-card">
              <h3>开设专业（示例 {{ majors.length }} 个）</h3>
              <ul class="gkd-majors">
                <li v-for="major in majors" :key="major.code" @click="router.push(`/majors/${major.code}`)">
                  <span class="gkd-majors__name">{{ major.name }}</span>
                  <span class="gkd-majors__meta">{{ major.category }} · {{ major.duration }} · {{ major.degree }}</span>
                  <span class="gkd-majors__score">
                    参考分 <b>{{ majorCutoff(school, major, opts)?.score }}</b>
                  </span>
                  <span class="gkd-majors__more">专业详情 &gt;</span>
                </li>
              </ul>
            </section>

            <!-- 招生计划 -->
            <section v-else class="gkd-card">
              <h3>{{ profile.province }} 招生计划（{{ profile.firstSubject }}类 · {{ profile.batch }}）</h3>
              <table class="gkd-table">
                <thead>
                  <tr>
                    <th>专业</th>
                    <th>专业代码</th>
                    <th>计划数</th>
                    <th>学制</th>
                    <th>学费/年</th>
                    <th>参考最低分</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in planRows" :key="row.code">
                    <td>{{ row.name }}</td>
                    <td>{{ row.code }}</td>
                    <td><b>{{ row.count }}</b></td>
                    <td>{{ row.duration }}</td>
                    <td>¥{{ row.tuition }}</td>
                    <td>{{ row.score }}（位次 {{ fmt(row.minRank) }}）</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </template>

          <p v-else class="gks-empty">没有找到这所院校，请返回查大学重新选择</p>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
