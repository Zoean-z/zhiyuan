<script setup>
import { computed } from "vue";
import { UI_TEXT } from "../utils/ui";

const props = defineProps({
  aiSummary: { type: String, default: "" },
  summary: { type: String, default: "" },
  finalAdvice: { type: String, default: "" },
  tips: { type: Array, default: () => [] }
});

const displaySummary = computed(() => props.aiSummary || props.summary || "");
const displayFinalAdvice = computed(() => props.finalAdvice || "");
const displayTips = computed(() => (Array.isArray(props.tips) ? props.tips.filter(Boolean) : []));
</script>

<template>
  <el-card class="summary-panel" shadow="never">
    <template #header>
      <div class="panel-title-row">
        <span>AI 总结</span>
        <el-tag size="small" type="info" effect="plain">报考建议</el-tag>
      </div>
    </template>
    <div v-if="displayTips.length" class="summary-tips">
      <div class="summary-tips__title">推荐提示</div>
      <ul class="summary-tips__list">
        <li v-for="(tip, idx) in displayTips" :key="idx">{{ tip }}</li>
      </ul>
    </div>
    <div v-if="displayFinalAdvice" class="summary-block">
      <div class="summary-block__title">规则建议</div>
      <div class="summary-text">{{ displayFinalAdvice }}</div>
    </div>
    <div v-if="displaySummary" class="summary-block">
      <div class="summary-block__title">AI 润色总结</div>
      <div class="summary-text">{{ displaySummary }}</div>
    </div>
    <el-empty v-else-if="!displayTips.length && !displayFinalAdvice" :description="UI_TEXT.empty.aiSummary" :image-size="90" />
  </el-card>
</template>
