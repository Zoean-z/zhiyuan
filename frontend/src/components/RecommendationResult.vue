<script setup>
import { computed, ref } from "vue";
import AiSummaryPanel from "./AiSummaryPanel.vue";
import UniversityCard from "./UniversityCard.vue";

const props = defineProps({
  loading: { type: Boolean, default: false },
  grouped: { type: Object, required: true },
  aiSummary: { type: String, default: "" },
  summary: { type: String, default: "" },
  rankMeta: { type: Object, default: null },
  showSaveAction: { type: Boolean, default: false },
  saveDisabled: { type: Boolean, default: false }
});

const emit = defineEmits(["save-plan"]);
const activeTab = ref("rush");
const rushList = computed(() => (Array.isArray(props.grouped?.rush) ? props.grouped.rush : []));
const safeList = computed(() => (Array.isArray(props.grouped?.safe) ? props.grouped.safe : []));
const guaranteeList = computed(() => (Array.isArray(props.grouped?.guarantee) ? props.grouped.guarantee : []));
const hasAnyData = computed(() => rushList.value.length + safeList.value.length + guaranteeList.value.length > 0);
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
</script>

<template>
  <section class="result-page">
    <el-card class="result-hero" shadow="never">
      <h2>推荐结果</h2>
      <p>按冲刺、稳妥、保底查看推荐院校，统一对比录取位次与位次差。</p>
    </el-card>

    <el-card v-if="showRankPanel" class="rank-panel" shadow="never">
      <template #header>
        <div class="panel-title-row">
          <span>用户位次信息</span>
        </div>
      </template>

      <div class="rank-panel__grid">
        <div class="rank-metric">
          <span>查询分数</span>
          <strong>{{ resolvedRankMeta.score ?? "-" }}</strong>
        </div>
        <div class="rank-metric">
          <span>考生省份</span>
          <strong>{{ resolvedRankMeta.province || "-" }}</strong>
        </div>
        <div class="rank-metric">
          <span>科类</span>
          <strong>{{ resolvedRankMeta.subjectTypeLabel || "-" }}</strong>
        </div>
        <div class="rank-metric">
          <span>用户位次</span>
          <strong>{{ resolvedRankMeta.userRank ?? "-" }}</strong>
        </div>
      </div>

      <p class="rank-panel__hint">
        <template v-if="resolvedRankMeta.userRank != null">
          已根据当前分数换算用户位次，下面的院校结果统一按位次差展示。
        </template>
        <template v-else>
          当前未命中可用位次映射，暂时无法展示位次推荐结果。
        </template>
      </p>
    </el-card>

    <el-card class="result-main" shadow="never">
      <template #header>
        <div class="panel-title-row result-main__header">
          <div class="panel-title-row">
            <span>院校推荐</span>
            <el-tag size="small" type="primary" effect="plain">冲刺 / 稳妥 / 保底</el-tag>
          </div>
          <el-button v-if="showSaveAction" type="primary" plain :disabled="saveDisabled" @click="emit('save-plan')">
            保存为志愿方案
          </el-button>
        </div>
      </template>

      <el-skeleton :loading="loading" animated>
        <template #template>
          <div class="cards-grid">
            <el-skeleton-item variant="p" style="height: 140px; border-radius: 12px;" />
            <el-skeleton-item variant="p" style="height: 140px; border-radius: 12px;" />
            <el-skeleton-item variant="p" style="height: 140px; border-radius: 12px;" />
          </div>
        </template>

        <template #default>
          <el-tabs v-model="activeTab" class="recommend-tabs">
            <el-tab-pane :label="'冲刺 (' + rushList.length + ')'" name="rush">
              <div v-if="rushList.length" class="cards-grid">
                <UniversityCard v-for="(item, idx) in rushList" :key="'rush-' + idx" :item="item" strategy="rush" />
              </div>
              <el-empty v-else description="暂无冲刺院校" :image-size="90" />
            </el-tab-pane>

            <el-tab-pane :label="'稳妥 (' + safeList.length + ')'" name="safe">
              <div v-if="safeList.length" class="cards-grid">
                <UniversityCard v-for="(item, idx) in safeList" :key="'safe-' + idx" :item="item" strategy="safe" />
              </div>
              <el-empty v-else description="暂无稳妥院校" :image-size="90" />
            </el-tab-pane>

            <el-tab-pane :label="'保底 (' + guaranteeList.length + ')'" name="guarantee">
              <div v-if="guaranteeList.length" class="cards-grid">
                <UniversityCard v-for="(item, idx) in guaranteeList" :key="'guarantee-' + idx" :item="item" strategy="guarantee" />
              </div>
              <el-empty v-else description="暂无保底院校" :image-size="90" />
            </el-tab-pane>
          </el-tabs>

          <el-empty v-if="!hasAnyData" description="暂无推荐数据，请先发起查询。" :image-size="100" />
        </template>
      </el-skeleton>
    </el-card>

    <AiSummaryPanel :ai-summary="aiSummary" :summary="summary" />
  </section>
</template>
