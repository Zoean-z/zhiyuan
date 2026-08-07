<script setup>
import { computed, ref, watch } from "vue";
import AiSummaryPanel from "./AiSummaryPanel.vue";
import UniversityCard from "./UniversityCard.vue";
import { buildPlanItemKey } from "../utils/recommendation";
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
const rushList = computed(() => (Array.isArray(props.grouped?.rush) ? props.grouped.rush : []));
const safeList = computed(() => (Array.isArray(props.grouped?.safe) ? props.grouped.safe : []));
const guaranteeList = computed(() => (Array.isArray(props.grouped?.guarantee) ? props.grouped.guarantee : []));
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
          <el-tabs v-model="activeTab" class="recommend-tabs">
            <el-tab-pane :label="'冲刺 ' + rushList.length" name="rush">
              <div v-if="rushList.length" class="cards-grid">
                <UniversityCard
                  v-for="(item, idx) in rushList"
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

            <el-tab-pane :label="'稳妥 ' + safeList.length" name="safe">
              <div v-if="safeList.length" class="cards-grid">
                <UniversityCard
                  v-for="(item, idx) in safeList"
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

            <el-tab-pane :label="'保底 ' + guaranteeList.length" name="guarantee">
              <div v-if="guaranteeList.length" class="cards-grid">
                <UniversityCard
                  v-for="(item, idx) in guaranteeList"
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
