<script setup>
import { computed } from "vue";
import { ChatDotRound } from "@element-plus/icons-vue";

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
  <section class="summary-panel">
    <header class="summary-panel__header">
      <span class="summary-panel__icon"><el-icon><ChatDotRound /></el-icon></span>
      <h3>AI 报考建议</h3>
    </header>
    <div class="summary-panel__content">
      <div v-if="displayTips.length" class="summary-block">
        <h4>推荐提示</h4>
        <ul class="summary-tips">
          <li v-for="(tip, idx) in displayTips" :key="idx">{{ tip }}</li>
        </ul>
      </div>
      <div v-if="displayFinalAdvice" class="summary-block">
        <h4>规则建议</h4>
        <p>{{ displayFinalAdvice }}</p>
      </div>
      <div v-if="displaySummary" class="summary-block summary-block--wide">
        <h4>AI 总结</h4>
        <p>{{ displaySummary }}</p>
      </div>
    </div>
  </section>
</template>
