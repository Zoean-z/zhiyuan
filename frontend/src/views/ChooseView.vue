<script setup>
import { Promotion } from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import {
  FIRST_SUBJECTS,
  SECOND_SUBJECTS,
  isReady,
  patchProfile,
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
 * 智能选大学：数据源为后端 /api/universities（80 所精选大学 + 比赛验证录取线 + 概率拆解）。
 * 概率/类型筛选、排序均在后端数据上生效；点击卡片进入院校详情（数据库 id）。
 */

const router = useRouter();
const route = useRoute();
const probability = ref("全部");
const typeFilter = ref("全部");
const sortKey = ref("概率");
const schools = ref([]);
const loading = ref(false);
const loadError = ref("");

onMounted(() => {
  syncFromAuth();
  // 首页「智能推荐大学」会带 score/subject/second/province 过来
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
  fetchSchools();
});

/* 分数/科类变化 → 重新拉取（后端实时重算概率） */
watch([score, rank, subjectType, () => profile.province], () => fetchSchools());

async function fetchSchools() {
  loading.value = true;
  loadError.value = "";
  try {
    const params = new URLSearchParams({
      examProvince: profile.province,
      subjectType: subjectType.value,
      withDataOnly: "true",
      size: "100"
    });
    if (score.value != null && Number(score.value) > 0) params.set("score", String(Number(score.value)));
    if (rank.value != null && Number(rank.value) > 0) params.set("userRank", String(Number(rank.value)));
    const resp = await fetch(`/api/universities?${params.toString()}`);
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const data = await resp.json();
    schools.value = (data.items || []).map((s) => {
      const prob = s.probability?.probability ?? null;
      const backendStrategy = String(s.probability?.strategy || "").toUpperCase();
      const strategy = {
        RUSH: { key: "rush", label: "冲", probabilityLabel: "概率小" },
        SAFE: { key: "safe", label: "稳", probabilityLabel: "概率中" },
        GUARANTEE: { key: "guard", label: "保", probabilityLabel: "概率大" }
      }[backendStrategy] || null;
      return {
        ...s,
        minScore: s.cutoffScore,
        minRank: s.minRank,
        probability: prob,
        probText: prob != null ? `${prob}%` : "待测算",
        rankGap: s.probability?.rankGap ?? null,
        scoreGap: s.probability?.scoreGap ?? null,
        strategyKey: strategy?.key ?? "unknown",
        prob: strategy?.probabilityLabel || "待测算"
      };
    });
  } catch (ex) {
    loadError.value = String(ex?.message || ex);
    schools.value = [];
  } finally {
    loading.value = false;
  }
}

const typeOpts = computed(() => ["全部", ...new Set(schools.value.map((s) => s.schoolType).filter(Boolean))]);
const PROB_OPTS = ["全部", "概率大", "概率中", "概率小"];
const SORT_OPTS = ["概率", "分数"];

const results = computed(() => {
  let list = schools.value;
  if (probability.value !== "全部") list = list.filter((item) => item.prob === probability.value);
  if (typeFilter.value !== "全部") list = list.filter((item) => item.schoolType === typeFilter.value);
  const probNum = (i) => i.probability ?? -1;
  if (sortKey.value === "概率") list = [...list].sort((a, b) => probNum(b) - probNum(a));
  else list = [...list].sort((a, b) => (b.minScore ?? -1) - (a.minScore ?? -1));
  return list;
});

function onScoreInput(event) {
  setScore(event.target.value);
}

function probClass(item) {
  if (item.probability == null) return "is-unknown";
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

/* 点击进院校详情（数据库真实 id，不再 404） */
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
              <p class="gk-choose__desc">输入科类、选科与分数，按「位次差 75% + 分差 25%」测算每所院校的参考录取概率</p>
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
                min="100"
                max="750"
                placeholder="高考分数"
                :value="profile.score ?? ''"
                @input="onScoreInput"
              />
              <i class="gk-filter__tip">位次按一分一段曲线自动换算，改分数即实时重算</i>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">概率</span>
              <button
                v-for="p in PROB_OPTS"
                :key="p"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': probability === p }"
                @click="probability = p"
              >
                {{ p }}
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">类型</span>
              <button
                v-for="t in typeOpts"
                :key="t"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': typeFilter === t }"
                @click="typeFilter = t"
              >
                {{ t }}
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">排序</span>
              <button
                v-for="s in SORT_OPTS"
                :key="s"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': sortKey === s }"
                @click="sortKey = s"
              >
                {{ s === "概率" ? "概率由高到低" : "分数由高到低" }}
              </button>
            </div>
          </div>

          <p v-if="isReady" class="gk-page__meta">
            {{ profile.province }} · {{ profile.firstSubject }}类 {{ score }} 分 ·
            <template v-if="rank != null">位次约 <b>{{ rank.toLocaleString() }}</b> ·</template>
            <template v-else>暂无位次数据 ·</template>
            匹配到
            <b>{{ results.length }}</b> 所院校
          </p>

          <ul v-if="isReady" class="gk-choose-list">
            <li v-for="item in results" :key="item.id" class="gk-choose" @click="openSchool(item)">
              <GkSchoolLogo :school="item" />
              <div class="gk-choose__info">
                <p class="gk-school__name">
                  {{ item.name }}
                  <span class="gk-school__loc">@{{ item.province }}</span>
                </p>
                <p class="gk-choose__rule">当前筛选：{{ item.schoolType || "综合" }} · {{ item.nature || "公办" }}</p>
                <p class="gk-school__tags">
                  <i v-for="tag in (item.schoolTags && item.schoolTags.length ? item.schoolTags : [item.schoolType, item.is985 ? '985' : '', (item.is211 || item.isDoubleFirstClass) ? '双一流' : '']).filter(Boolean)" :key="tag">{{ tag }}</i>
                </p>
              </div>
              <div class="gk-choose__nums">
                <p><b>{{ item.minScore == null ? "暂无" : item.minScore }}</b><span>最低分</span></p>
                <p><b>{{ item.minRank == null ? "待测" : item.minRank.toLocaleString() }}</b><span>最低位次</span></p>
                <p class="gk-choose__rate">
                  <b>{{ item.probText }}</b><span>录取概率</span>
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
            <li v-if="!results.length" class="gk-school__empty">没有匹配的院校，试试放宽概率筛选</li>
          </ul>

          <div v-else class="gk-choose__placeholder">
            <p class="gk-choose__ph-title">概率是怎么算出来的？</p>
            <p class="gk-choose__ph-desc">
              ① 先用当前省份、科类的一分一段数据把你的分数换成全省位次；
              ② 拿你的位次与院校最低位次相减得到「位次差」，分数与最低分相减得到「分差」；
              ③ 两个差值分段换算成概率后，按 位次 75% + 分数 25% 加权（与后端
              <code>RecommendationPolicyService</code> 完全一致）；
              ④ ≥75% 为保底、≥55% 为稳妥、≥35% 为冲击，低于 35% 判为高风险。
              当前院校线为比赛验证数据，只在对应省份和科类有覆盖时参与测算。
            </p>
            <p class="gk-choose__ph-desc">填入高考分数后，匹配结果会立即显示并随分数实时更新。</p>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
