<script setup>
import { Promotion, Search } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { CHOOSE_PROBABILITIES, RANK_LIST, schoolLoc, schoolTags } from "../utils/exploreData";
import { probDetailOf, schoolCutoff } from "../utils/volunteerCore";
import { strategyOf } from "../utils/scoreModel";
import {
  FIRST_SUBJECTS,
  SECOND_SUBJECTS,
  isReady,
  patchProfile,
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

/**
 * 智能选大学
 * 【修复】原来本页的分数/选科是局部 ref，首页面板填的东西传不过来；
 * 同时 buildChooseResults() 内部又是一套自己的假公式（701 - rank*7、位次 7200+rank*4860），
 * 与查大学/志愿表的数据完全矛盾。
 * 现在：考生信息走 examProfile，录取数据与概率走 volunteerCore + scoreModel（全站同一套）。
 */

const router = useRouter();
const route = useRoute();
const probability = ref("全部");
const submitted = ref(false);

onMounted(() => {
  syncFromAuth();
  // 首页「智能推荐大学」会带 score/rank/subject/second/province 过来
  const q = route.query;
  const patch = {};
  const presetScore = Number(Array.isArray(q.score) ? q.score[0] : q.score);
  if (Number.isFinite(presetScore) && presetScore > 0) patch.score = presetScore;
  if (typeof q.subject === "string" && FIRST_SUBJECTS.includes(q.subject)) patch.firstSubject = q.subject;
  if (typeof q.province === "string" && q.province) patch.province = q.province;
  if (typeof q.second === "string" && q.second) {
    patch.secondSubjects = q.second.split(",").filter((item) => SECOND_SUBJECTS.includes(item)).slice(0, 2);
  }
  if (Object.keys(patch).length) patchProfile(patch);
  if (isReady.value) submitted.value = true;
});

const opts = computed(() => ({
  province: profile.province,
  subjectType: subjectType.value,
  userRank: rank.value
}));

/** 概率档位 → 页面上的「概率大/中/小」 */
function probLabelOf(key) {
  if (key === "guard") return "概率大";
  if (key === "safe") return "概率中";
  return "概率小";
}

const allResults = computed(() =>
  RANK_LIST.map((school) => {
    const seed = school.id * 37;
    const group = ((seed % 24) + 6).toString().padStart(3, "0");
    const cutoff = schoolCutoff(school, opts.value);
    const detail = probDetailOf(school, score.value, opts.value);
    const strategy = detail.probability == null ? null : strategyOf(detail.probability);
    const second = profile.secondSubjects.length ? profile.secondSubjects.join(" / ") : "不限";
    return {
      ...school,
      group,
      rule: `首选${profile.firstSubject}，再选${second}`,
      minScore: cutoff?.score ?? null,
      minRank: cutoff?.minRank ?? null,
      probability: detail.probability,
      rankGap: detail.rankGap,
      scoreGap: detail.scoreGap,
      strategyKey: strategy?.key ?? "unknown",
      prob: strategy ? probLabelOf(strategy.key) : "待测算"
    };
  }).sort((a, b) => (b.probability ?? -1) - (a.probability ?? -1))
);

const results = computed(() =>
  probability.value === "全部" ? allResults.value : allResults.value.filter((item) => item.prob === probability.value)
);

function onScoreInput(event) {
  setScore(event.target.value);
}

function onRankInput(event) {
  const raw = event.target.value;
  profile.manualRank = raw === "" ? null : Number(raw);
}

function runChoose() {
  submitted.value = isReady.value;
}

function probClass(item) {
  if (item.prob === "概率大") return "is-high";
  if (item.prob === "概率中") return "is-mid";
  return "is-low";
}

function rankGapText(item) {
  if (item.rankGap == null) return "位次待测";
  return `位次${item.rankGap >= 0 ? "靠前" : "落后"} ${Math.abs(item.rankGap).toLocaleString()}`;
}

function scoreGapText(item) {
  if (item.scoreGap == null) return "分数待测";
  return `分数${item.scoreGap >= 0 ? "高出" : "不足"} ${Math.abs(item.scoreGap)} 分`;
}

function gapClass(value) {
  if (value == null) return "";
  return value >= 0 ? "is-up" : "is-down";
}

/* 【修复】原来「测概率」是拉起 AI 对话问一句，现在直接进院校详情页看近三年录取与概率拆解 */
function openSchool(item) {
  router.push({ name: "school-detail", params: { id: item.id } });
}

function goAgentPlan() {
  router.push({ path: "/agent", query: { q: "请按院校优先，帮我生成冲稳保涨度志愿方案" } });
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="智能选大学" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gk-choose__hero">
            <div>
              <h2 class="gk-choose__title">智能选大学</h2>
              <p class="gk-choose__desc">输入科类、选科与分数，按「位次差 75% + 分差 25%」测算每个专业组的录取概率</p>
            </div>
            <button class="gk-choose__ai" type="button" @click="goAgentPlan">
              <el-icon><Promotion /></el-icon>
              AI 方案版
            </button>
          </div>

          <div class="gk-filter">
            <div class="gk-filter__row">
              <span class="gk-filter__label">首选</span>
              <button
                v-for="s in FIRST_SUBJECTS"
                :key="s"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': profile.firstSubject === s }"
                @click="setFirstSubject(s)"
              >
                {{ s }}
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">再选</span>
              <button
                v-for="o in SECOND_SUBJECTS"
                :key="o"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': profile.secondSubjects.includes(o) }"
                @click="toggleSecondSubject(o)"
              >
                {{ o }}
              </button>
              <i class="gk-filter__tip">最多选 2 门</i>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">分数</span>
              <input
                class="gk-choose__input"
                type="number"
                min="0"
                max="750"
                placeholder="高考分数"
                :value="profile.score ?? ''"
                @input="onScoreInput"
              />
              <span class="gk-filter__label gk-choose__label2">位次</span>
              <input
                class="gk-choose__input"
                type="number"
                placeholder="选填，默认自动换算"
                :value="profile.manualRank ?? ''"
                @input="onRankInput"
              />
              <button class="gk-choose__run" type="button" @click="runChoose">
                <el-icon><Search /></el-icon>
                开始匹配
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">概率</span>
              <button
                v-for="p in CHOOSE_PROBABILITIES"
                :key="p"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': probability === p }"
                @click="probability = p"
              >
                {{ p }}
              </button>
            </div>
          </div>

          <p v-if="submitted && isReady" class="gk-page__meta">
            {{ profile.province }} · {{ profile.firstSubject }}类 {{ score }} 分 · 位次约
            <b>{{ rank.toLocaleString() }}</b>（超过 {{ percent }}% 考生）· 匹配到
            <b>{{ results.length }}</b> 个专业组
          </p>

          <ul v-if="submitted && isReady" class="gk-choose-list">
            <li v-for="item in results" :key="`${item.id}-${item.group}`" class="gk-choose" @click="openSchool(item)">
              <GkSchoolLogo :school="item" />
              <div class="gk-choose__info">
                <p class="gk-school__name">
                  {{ item.name }}
                  <span class="gk-school__loc">@{{ schoolLoc(item) }}</span>
                  <span class="gk-choose__group">专业组({{ item.group }})</span>
                </p>
                <p class="gk-choose__rule">选科规则：{{ item.rule }}</p>
                <p class="gk-school__tags">
                  {{ item.type }} | {{ item.nature }}
                  <i v-for="tag in schoolTags(item)" :key="tag">{{ tag }}</i>
                </p>
              </div>
              <div class="gk-choose__nums">
                <p><b>{{ item.minScore }}</b><span>最低分</span></p>
                <p><b>{{ item.minRank == null ? "待测" : item.minRank.toLocaleString() }}</b><span>最低位次</span></p>
                <p class="gk-choose__rate">
                  <b>{{ item.probability }}%</b><span>录取概率</span>
                </p>
              </div>
              <div class="gk-choose__gap">
                <span :class="gapClass(item.rankGap)">
                  {{ rankGapText(item) }}
                </span>
                <span :class="gapClass(item.scoreGap)">
                  {{ scoreGapText(item) }}
                </span>
              </div>
              <span class="gk-choose__prob" :class="probClass(item)">{{ item.prob }}</span>
              <button class="gk-school__action" type="button" @click.stop="openSchool(item)">院校详情 &gt;</button>
            </li>
            <li v-if="!results.length" class="gk-school__empty">没有匹配的专业组，试试放宽概率筛选</li>
          </ul>

          <div v-else class="gk-choose__placeholder">
            <p class="gk-choose__ph-title">概率是怎么算出来的？</p>
            <p class="gk-choose__ph-desc">
              ① 先用一分一段曲线把你的分数换成全省位次；
              ② 拿你的位次与院校历年最低位次相减得到「位次差」，分数与最低分相减得到「分差」；
              ③ 两个差值分段换算成概率后，按 位次 75% + 分数 25% 加权（与后端
              <code>RecommendationPolicyService</code> 完全一致）；
              ④ ≥75% 为保底、≥55% 为稳妥、≥35% 为冲击，低于 35% 判为高风险。
            </p>
            <p class="gk-choose__ph-desc">请先在上方填入高考分数，再点「开始匹配」。</p>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
