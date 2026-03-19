<script setup>
import { computed } from "vue";

const props = defineProps({
  aiSummary: { type: String, default: "" },
  summary: { type: String, default: "" }
});

const displaySummary = computed(() => props.aiSummary || props.summary || "");
</script>

<template>
  <el-card class="summary-panel" shadow="never">
    <template #header>
      <div class="panel-title-row">
        <span>AI 总结</span>
        <el-tag size="small" type="info" effect="plain">报考建议</el-tag>
      </div>
    </template>
    <div v-if="displaySummary" class="summary-text">{{ displaySummary }}</div>
    <el-empty v-else description="暂无 AI 总结" :image-size="90" />
  </el-card>
</template>