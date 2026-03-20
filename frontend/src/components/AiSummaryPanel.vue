<script setup>
import { computed } from "vue";

const props = defineProps({
  aiSummary: { type: String, default: "" },
  summary: { type: String, default: "" },
  tips: { type: Array, default: () => [] }
});

const displaySummary = computed(() => props.aiSummary || props.summary || "");
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
    <div v-if="displaySummary" class="summary-text">{{ displaySummary }}</div>
    <el-empty v-else-if="!displayTips.length" description="暂无 AI 总结" :image-size="90" />
  </el-card>
</template>
