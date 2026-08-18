<script setup>
import { computed, ref, watch } from "vue";
import { Search } from "@element-plus/icons-vue";
import AiSummaryPanel from "./AiSummaryPanel.vue";
import RecommendSchoolRow from "./RecommendSchoolRow.vue";
import { buildPlanItemKey, normalizeSchoolTags } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";

const props = defineProps({
  loading: { type: Boolean, default: false },
  grouped: { type: Object, required: true },
  aiSummary: { type: String, default: "" },
  summary: { type: String, default: "" },
  finalAdvice: { type: String, default: "" },
  tips: { type: Array, default: () => [] },
  recommendationMode: { type: String, default: "" },
  rankMeta: { type: Object, default: null },
  showAddAction: { type: Boolean, default: false },
  showAiSummary: { type: Boolean, default: false },
  selectedPlanKeys: { type: Array, default: () => [] }
});

const emit = defineEmits(["add-item", "view-school-detail"]);
const activeTab = ref("rush");
const typeFilter = ref("ALL");
const keyword = ref("");
const TYPE_FILTERS = [
  { value: "ALL", label: "类型不限" },
  { value: "985", label: "985" },
  { value: "211", label: "211" },
  { value: "DOUBLE", label: "双一流" }
];
const rushList = computed(() => (Array.isArray(props.grouped?.rush) ? props.grouped.rush : []));
const safeList = computed(() => (Array.isArray(props.grouped?.safe) ? props.grouped.safe : []));
const guaranteeList = computed(() => (Array.isArray(props.grouped?.guarantee) ? props.grouped.guarantee : []));
function filterList(list) {
  const normalizedKeyword = keyword.value.trim().toLowerCase();
  return list.filter((item) => {
    const tags = normalizeSchoolTags(item);
    const typeMatches = typeFilter.value === "ALL"
      || (typeFilter.value === "985" && tags.is985)
      || (typeFilter.value === "211" && tags.is211)
      || (typeFilter.value === "DOUBLE" && tags.isDoubleFirstClass);
    const keywordMatches = !normalizedKeyword
      || String(item.universityName || item.schoolName || "").toLowerCase().includes(normalizedKeyword)
      || String(item.majorName || "").toLowerCase().includes(normalizedKeyword);
    return typeMatches && keywordMatches;
  });
}
const filteredRush = computed(() => filterList(rushList.value));
const filteredSafe = computed(() => filterList(safeList.value));
const filteredGuarantee = computed(() => filterList(guaranteeList.value));
const totalCount = computed(() => rushList.value.length + safeList.value.length + guaranteeList.value.length);
const hasAnyRankFields = computed(() => {
  const firstItem = rushList.value[0] || safeList.value[0] || guaranteeList.value[0] || null;
  return !!firstItem && (
    firstItem.userRank != null
    || firstItem.minRank != null
    || firstItem.rankGap != null
    || !!firstItem.recommendationBasis
  );
});
const resolvedRankMeta = computed(() => {
  const meta = props.rankMeta || {};
  const firstItem = rushList.value[0] || safeList.value[0] || guaranteeList.value[0] || {};
  return {
    score: meta.score ?? null,
    province: meta.province ?? "",
    subjectTypeLabel: meta.subjectTypeLabel ?? "",
    userRank: meta.userRank ?? firstItem.userRank ?? null,
    recommendationBasis: meta.recommendationBasis ?? firstItem.recommendationBasis ?? ""
  };
});
const showRankPanel = computed(() =>
  !!props.rankMeta
  || hasAnyRankFields.value
  || resolvedRankMeta.value.userRank != null
);
const resolvedRecommendationMode = computed(() => {
  if (props.recommendationMode) {
    return props.recommendationMode;
  }
  const firstItem = rushList.value[0] || safeList.value[0] || guaranteeList.value[0] || null;
  return firstItem?.recommendationMode || "SCHOOL_FIRST";
});
const resultTargetText = computed(() => resolvedRecommendationMode.value === "MAJOR_FIRST" ? "学校+专业" : "院校");
const selectedKeySet = computed(() => new Set(props.selectedPlanKeys));

watch(
  [rushList, safeList, guaranteeList],
  ([rush, safe, guarantee]) => {
    const lists = { rush, safe, guarantee };
    if (lists[activeTab.value]?.length) return;
    activeTab.value = ["rush", "safe", "guarantee"].find((key) => lists[key].length) || "rush";
  },
  { immediate: true }
);

function isItemAdded(item, strategy) {
  return selectedKeySet.value.has(buildPlanItemKey(item, strategy));
}
</script>

<template>
  <section class="result-page">
    <div class="result-main">
      <el-skeleton :loading="loading" animated>
        <template #template>
          <div class="cards-grid">
            <el-skeleton-item v-for="index in 4" :key="index" variant="p" class="recommend-card-skeleton" />
          </div>
        </template>

        <template #default>
          <div v-if="totalCount" class="mnz-result-toolbar">
            <div>共 <strong>{{ totalCount }}</strong> 条推荐
              <span class="is-rush">冲刺 {{ rushList.length }}</span>
              <span class="is-safe">稳妥 {{ safeList.length }}</span>
              <span class="is-guarantee">保底 {{ guaranteeList.length }}</span>
            </div>
            <div class="mnz-result-toolbar__filters">
              <el-select v-model="typeFilter" size="small">
                <el-option v-for="option in TYPE_FILTERS" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
              <el-input v-model="keyword" size="small" clearable placeholder="搜索院校或专业" :prefix-icon="Search" />
            </div>
          </div>
          <el-tabs v-model="activeTab" class="recommend-tabs">
            <el-tab-pane :label="'冲刺 ' + filteredRush.length" name="rush">
              <div v-if="filteredRush.length" class="mnz-school-list">
                <RecommendSchoolRow
                  v-for="(item, idx) in filteredRush"
                  :key="'rush-' + idx"
                  :item="item"
                  strategy="rush"
                  :show-add-action="showAddAction"
                  :added="isItemAdded(item, 'rush')"
                  @add="emit('add-item', item, 'rush')"
                  @view-detail="emit('view-school-detail', item, 'rush')"
                />
              </div>
              <el-empty v-else :description="UI_TEXT.empty.noRush + resultTargetText" :image-size="90" />
            </el-tab-pane>

            <el-tab-pane :label="'稳妥 ' + filteredSafe.length" name="safe">
              <div v-if="filteredSafe.length" class="mnz-school-list">
                <RecommendSchoolRow
                  v-for="(item, idx) in filteredSafe"
                  :key="'safe-' + idx"
                  :item="item"
                  strategy="safe"
                  :show-add-action="showAddAction"
                  :added="isItemAdded(item, 'safe')"
                  @add="emit('add-item', item, 'safe')"
                  @view-detail="emit('view-school-detail', item, 'safe')"
                />
              </div>
              <el-empty v-else :description="UI_TEXT.empty.noSafe + resultTargetText" :image-size="90" />
            </el-tab-pane>

            <el-tab-pane :label="'保底 ' + filteredGuarantee.length" name="guarantee">
              <div v-if="filteredGuarantee.length" class="mnz-school-list">
                <RecommendSchoolRow
                  v-for="(item, idx) in filteredGuarantee"
                  :key="'guarantee-' + idx"
                  :item="item"
                  strategy="guarantee"
                  :show-add-action="showAddAction"
                  :added="isItemAdded(item, 'guarantee')"
                  @add="emit('add-item', item, 'guarantee')"
                  @view-detail="emit('view-school-detail', item, 'guarantee')"
                />
              </div>
              <el-empty v-else :description="UI_TEXT.empty.noGuarantee + resultTargetText" :image-size="90" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-skeleton>
    </div>

    <AiSummaryPanel
      v-if="showAiSummary && (aiSummary || summary || finalAdvice || tips.length)"
      :ai-summary="aiSummary"
      :summary="summary"
      :final-advice="finalAdvice"
      :tips="tips"
    />
  </section>
</template>
