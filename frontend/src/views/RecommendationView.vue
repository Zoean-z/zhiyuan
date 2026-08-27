<script setup>
import { computed, inject, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import RecommendationResult from "../components/RecommendationResult.vue";
import SchoolDetailDrawer from "../components/SchoolDetailDrawer.vue";
import MajorPickDialog from "../components/MajorPickDialog.vue";
import { RECOMMENDATION_MODE_OPTIONS, SUBJECT_OPTIONS, recommendationModeLabel } from "../utils/recommendation";

const workspace = inject("workspace");
const {
  activeMode, addCurrentPlanItem, addSelectedMajorsToPlan, aiSummary, error,
  finalAdvice, grouped, latestRankMeta, latestResult, latestSourceType,
  loadCurrentPlanDraft, loadMajorSuggestions, loading, majorSuggestionLoading,
  openSchoolDetail, provinces, queryByScore, queryByText,
  resultSummary, resultTips, schoolDetail, schoolDetailLoading, schoolDetailMajors,
  schoolDetailVisible, scoreForm, selectedPlanKeys, textForm, textParsedRequirement
} = workspace;

const recognizedConditions = computed(() => {
  const parsed = textParsedRequirement.value || {};
  const values = [];
  if (parsed.recommendationMode) values.push(recommendationModeLabel(parsed.recommendationMode));
  if (Array.isArray(parsed.provinces)) values.push(...parsed.provinces.slice(0, 2));
  if (Array.isArray(parsed.normalizedMajors) && parsed.normalizedMajors.length) {
    values.push(...parsed.normalizedMajors.slice(0, 2));
  } else if (Array.isArray(parsed.majorKeywords)) {
    values.push(...parsed.majorKeywords.slice(0, 2));
  }
  if (parsed.riskPreference) values.push(parsed.riskPreference);
  return Array.from(new Set(values.filter(Boolean))).slice(0, 6);
});

const route = useRoute();
const router = useRouter();

const MODE_TABS = [
  { key: "text", label: "文本查询", desc: "一句话描述需求" },
  { key: "score", label: "分数查询", desc: "精确分数定位" }
];

function applyModeFromQuery() {
  const mode = Array.isArray(route.query.mode) ? route.query.mode[0] : route.query.mode;
  if (mode !== "SCHOOL_FIRST" && mode !== "MAJOR_FIRST") return;
  activeMode.value = "score";
  scoreForm.recommendationMode = mode;
}

onMounted(() => {
  applyModeFromQuery();
  loadCurrentPlanDraft();
});

function goAgentPlan() {
  router.push({ path: "/agent", query: { q: "帮我定位目标院校，生成一份冲稳保志愿方案" } });
}

/* 可填专业 → 投放志愿表（与志愿填报器联动） */
const majorPickVisible = ref(false);
const majorPickItem = ref(null);
const majorPickStrategy = ref("safe");
function openMajorPick(item, strategy) {
  majorPickItem.value = item;
  majorPickStrategy.value = strategy || "safe";
  majorPickVisible.value = true;
}

const latestRecommendationMode = computed(() => latestResult.value?.recommendationMode
  || latestResult.value?.parsed?.recommendationMode
  || "");
const resultMatchesActiveMode = computed(() => {
  if (latestSourceType.value !== activeMode.value) return false;
  if (activeMode.value !== "score") return true;
  return !latestRecommendationMode.value
    || latestRecommendationMode.value === scoreForm.recommendationMode;
});
const displayedGrouped = computed(() => resultMatchesActiveMode.value
  ? grouped
  : { rush: [], safe: [], guarantee: [] });
const displayedRecommendationMode = computed(() => {
  if (!resultMatchesActiveMode.value) return scoreForm.recommendationMode;
  return latestRecommendationMode.value
    || scoreForm.recommendationMode;
});
const majorPickProvince = computed(() => {
  if (activeMode.value === "score") return scoreForm.province || "";
  return latestResult.value?.parsed?.candidateProvince
    || latestResult.value?.parsed?.province
    || "";
});
const majorPickSubjectType = computed(() => {
  if (activeMode.value === "score") return scoreForm.subjectType || "";
  return latestResult.value?.parsed?.subjectType || "";
});
const currentSubjectLabel = computed(() => SUBJECT_OPTIONS.find((item) => item.value === scoreForm.subjectType)?.label || "");
const scoreRankSummary = computed(() => {
  const meta = latestRankMeta.value;
  if (!meta || activeMode.value !== "score" || !resultMatchesActiveMode.value || meta.userRank == null) return null;
  if (Number(meta.score) !== Number(scoreForm.score)) return null;
  if ((meta.province || "") !== (scoreForm.province || "")) return null;
  if ((meta.subjectTypeLabel || "") !== currentSubjectLabel.value) return null;
  return meta;
});

async function queryMajorSuggestions(queryString, callback) {
  const suggestions = await loadMajorSuggestions(queryString);
  callback(suggestions.map((value) => ({ value })));
}

function formatRank(value) {
  return Number(value).toLocaleString("zh-CN");
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="智能选大学" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content gk-rec">
          <div class="mnz-rec">
            <div class="mnz-rec__head">
              <div class="mnz-rec__heading">
                <h2 class="mnz-rec__title">智能推荐查询</h2>
                <p class="mnz-rec__desc">输入分数或一句话需求，AI 生成冲稳保院校推荐，一键加入志愿表</p>
              </div>
              <button type="button" class="mnz-wb__ai" @click="goAgentPlan"><i>AI</i>对话式定制</button>
            </div>

            <div class="mnz-rec__modes">
              <button
                v-for="tab in MODE_TABS"
                :key="tab.key"
                type="button"
                class="mnz-rec__mode"
                :class="{ 'is-active': activeMode === tab.key }"
                @click="activeMode = tab.key"
              >
                <strong>{{ tab.label }}</strong>
                <span>{{ tab.desc }}</span>
              </button>
            </div>

            <div class="mnz-rec__panel">
              <template v-if="activeMode === 'text'">
                <div class="mnz-rec__text">
                  <el-input
                    v-model.trim="textForm.requirementText"
                    type="textarea"
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                    resize="none"
                    placeholder="输入分数、科类、意向专业、地区或风险偏好"
                  />
                  <button type="button" class="mnz-rec__go" :disabled="loading" @click="queryByText">
                    <span>{{ loading ? "生成中…" : "开始推荐" }}</span>
                  </button>
                </div>

                <div v-if="recognizedConditions.length" class="mnz-rec__tags">
                  <span class="mnz-rec__tags-label">已识别条件</span>
                  <span v-for="item in recognizedConditions" :key="item" class="mnz-rec__tag">{{ item }}</span>
                </div>
              </template>

              <template v-else>
                <div class="mnz-rec__seg">
                  <button
                    v-for="opt in RECOMMENDATION_MODE_OPTIONS"
                    :key="opt.value"
                    type="button"
                    class="mnz-rec__seg-btn"
                    :class="{ 'is-active': scoreForm.recommendationMode === opt.value }"
                    @click="scoreForm.recommendationMode = opt.value"
                  >
                    {{ opt.label }}
                  </button>
                </div>

                <div class="mnz-rec__fields">
                  <label class="mnz-rec__field">
                    <span>分数</span>
                    <el-input v-model="scoreForm.score" type="number" placeholder="650" />
                  </label>
                  <label class="mnz-rec__field">
                    <span>省份</span>
                    <el-select v-model="scoreForm.province" placeholder="请选择">
                      <el-option v-for="province in provinces" :key="province" :label="province" :value="province" />
                    </el-select>
                  </label>
                  <label class="mnz-rec__field">
                    <span>科类</span>
                    <el-select v-model="scoreForm.subjectType" placeholder="请选择">
                      <el-option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                    </el-select>
                  </label>
                  <label v-if="scoreForm.recommendationMode === 'MAJOR_FIRST'" class="mnz-rec__field mnz-rec__field--major">
                    <span>专业</span>
                    <el-autocomplete
                      v-model="scoreForm.majorKeyword"
                      clearable
                      :fetch-suggestions="queryMajorSuggestions"
                      :trigger-on-focus="false"
                      :loading="majorSuggestionLoading"
                      placeholder="输入专业名称"
                    />
                  </label>
                  <button type="button" class="mnz-rec__go" :disabled="loading" @click="queryByScore">
                    <span>{{ loading ? "生成中…" : "开始推荐" }}</span>
                  </button>
                </div>
              </template>

              <div v-if="error" class="mnz-rec__error">{{ error }}</div>
            </div>

            <div v-if="scoreRankSummary" class="mnz-rec__rank" aria-live="polite">
              <div class="mnz-rec__rank-item">
                <span>本次成绩</span>
                <strong>{{ scoreRankSummary.score }} 分</strong>
              </div>
              <div class="mnz-rec__rank-item mnz-rec__rank-item--ctx">
                <span>{{ scoreRankSummary.province }} · {{ scoreRankSummary.subjectTypeLabel }}</span>
              </div>
              <div class="mnz-rec__rank-item">
                <span>对应位次</span>
                <strong>{{ formatRank(scoreRankSummary.userRank) }}</strong>
              </div>
            </div>
          </div>

          <RecommendationResult
            :loading="loading"
            :grouped="displayedGrouped"
            :summary="resultSummary"
            :ai-summary="aiSummary"
            :final-advice="finalAdvice"
            :tips="resultTips"
            :recommendation-mode="displayedRecommendationMode"
            :rank-meta="latestRankMeta"
            :show-add-action="true"
            :show-ai-summary="resultMatchesActiveMode && latestSourceType === 'text'"
            :selected-plan-keys="selectedPlanKeys"
            @add-item="addCurrentPlanItem"
            @view-school-detail="openSchoolDetail"
            @pick-majors="openMajorPick"
          />
        </section>

        <GkSidePanel />
      </div>
    </main>

    <SchoolDetailDrawer
      v-model="schoolDetailVisible"
      :loading="schoolDetailLoading"
      :school="schoolDetail"
      :majors="schoolDetailMajors"
      @add-selected="addSelectedMajorsToPlan"
    />

    <MajorPickDialog
      v-model:visible="majorPickVisible"
      :item="majorPickItem"
      :strategy="majorPickStrategy"
      :province="majorPickProvince"
      :subject-type="majorPickSubjectType"
    />
  </div>
</template>
