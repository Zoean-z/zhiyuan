<script setup>
import { computed, inject, onActivated } from "vue";
import { useRouter } from "vue-router";
import RecommendationResult from "../components/RecommendationResult.vue";
import SchoolDetailDrawer from "../components/SchoolDetailDrawer.vue";
import { RECOMMENDATION_MODE_OPTIONS, SUBJECT_OPTIONS, recommendationModeLabel } from "../utils/recommendation";

const workspace = inject("workspace");
const router = useRouter();
const MODE_TABS = [
  { key: "text", label: "文本查询", desc: "一句话描述报考需求" },
  { key: "score", label: "分数查询", desc: "按成绩与位次精确定位" }
];
const {
  activeMode, addCurrentPlanItem, addSelectedMajorsToPlan, aiSummary, auth, error,
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

function goAgentPlan() {
  router.push({ path: "/agent", query: { q: "帮我定位目标院校，生成一份冲稳保志愿方案" } });
}

onActivated(() => {
  loadCurrentPlanDraft();
});
</script>

<template>
  <el-main class="app-main recommendation-view">
    <section class="mnz-rec">
      <div class="mnz-rec__head">
        <div>
          <span class="mnz-rec__eyebrow">智能志愿推荐</span>
        </div>
        <button type="button" class="mnz-rec__ai" @click="goAgentPlan"><strong>AI</strong> 对话式定制</button>
      </div>

      <div class="mnz-rec__modes" role="tablist" aria-label="推荐查询方式">
        <button v-for="tab in MODE_TABS" :key="tab.key" type="button" role="tab"
          :aria-selected="activeMode === tab.key" :class="['mnz-rec__mode', { 'is-active': activeMode === tab.key }]"
          @click="activeMode = tab.key">
          <strong>{{ tab.label }}</strong><span>{{ tab.desc }}</span>
        </button>
      </div>

      <div class="mnz-rec__panel">
        <template v-if="activeMode === 'text'">
          <div class="text-query-row">
            <el-input
              v-model.trim="textForm.requirementText"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="输入分数、科类、意向专业、地区或风险偏好"
            />
            <el-button type="primary" class="query-action" :loading="loading" @click="queryByText">开始推荐</el-button>
          </div>

          <div v-if="recognizedConditions.length" class="recognized-inline">
            <span>已识别条件：</span>
            <el-tag v-for="item in recognizedConditions" :key="item" effect="light">{{ item }}</el-tag>
          </div>
        </template>

        <template v-else>
          <div class="score-query-panel">
            <el-radio-group v-model="scoreForm.recommendationMode" class="priority-switch">
              <el-radio-button v-for="opt in RECOMMENDATION_MODE_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio-button>
            </el-radio-group>

            <div class="score-query-fields">
              <label class="query-field">
                <span>分数</span>
                <el-input v-model="scoreForm.score" type="number" placeholder="650" />
              </label>
              <label class="query-field">
                <span>省份</span>
                <el-select v-model="scoreForm.province" placeholder="请选择">
                  <el-option v-for="province in provinces" :key="province" :label="province" :value="province" />
                </el-select>
              </label>
              <label class="query-field">
                <span>科类</span>
                <el-select v-model="scoreForm.subjectType" placeholder="请选择">
                  <el-option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </label>
              <label v-if="scoreForm.recommendationMode === 'MAJOR_FIRST'" class="query-field query-field--major">
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
              <el-button type="primary" class="query-action" :loading="loading" @click="queryByScore">开始推荐</el-button>
            </div>
          </div>
        </template>
        <div v-if="error" class="error query-error">{{ error }}</div>
      </div>
    </section>

    <section v-if="scoreRankSummary" class="score-rank-summary" aria-live="polite">
      <div class="score-rank-summary__lead">
        <span>本次成绩</span>
        <strong>{{ scoreRankSummary.score }} 分</strong>
      </div>
      <div class="score-rank-summary__context">
        <span>{{ scoreRankSummary.province }}</span>
        <i aria-hidden="true" />
        <span>{{ scoreRankSummary.subjectTypeLabel }}</span>
      </div>
      <div class="score-rank-summary__rank">
        <span>对应位次</span>
        <strong>{{ formatRank(scoreRankSummary.userRank) }}</strong>
      </div>
    </section>

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
    />

    <SchoolDetailDrawer
      v-model="schoolDetailVisible"
      :loading="schoolDetailLoading"
      :school="schoolDetail"
      :majors="schoolDetailMajors"
      :profile="auth?.user"
      @add-selected="addSelectedMajorsToPlan"
    />
  </el-main>
</template>
