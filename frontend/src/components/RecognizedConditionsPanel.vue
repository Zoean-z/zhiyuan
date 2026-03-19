<script setup>
import { computed } from "vue";
import { recommendationModeLabel } from "../utils/recommendation";

const props = defineProps({
  parsed: { type: Object, default: null }
});

const rows = computed(() => {
  const parsed = props.parsed || {};
  return [
    { label: "推荐模式", value: parsed.recommendationMode ? recommendationModeLabel(parsed.recommendationMode) : "-" },
    { label: "学校层次", value: Array.isArray(parsed.schoolLevels) && parsed.schoolLevels.length ? parsed.schoolLevels.join("、") : "-" },
    { label: "院校类型", value: Array.isArray(parsed.schoolTypes) && parsed.schoolTypes.length ? parsed.schoolTypes.join("、") : "-" },
    { label: "地区", value: Array.isArray(parsed.provinces) && parsed.provinces.length ? parsed.provinces.join("、") : "-" },
    { label: "专业关键词", value: Array.isArray(parsed.majorKeywords) && parsed.majorKeywords.length ? parsed.majorKeywords.join("、") : "-" },
    { label: "标准专业", value: Array.isArray(parsed.normalizedMajors) && parsed.normalizedMajors.length ? parsed.normalizedMajors.join("、") : "-" },
    { label: "风险偏好", value: parsed.riskPreference || "-" },
    { label: "未识别偏好", value: Array.isArray(parsed.unrecognizedPreferences) && parsed.unrecognizedPreferences.length ? parsed.unrecognizedPreferences.join("、") : "-" }
  ];
});
</script>

<template>
  <el-card class="recognized-card" shadow="never">
    <template #header>
      <div class="panel-title-row">
        <span>已识别条件</span>
        <el-tag size="small" type="info" effect="plain">自由文本解析</el-tag>
      </div>
    </template>
    <div class="recognized-grid">
      <div v-for="row in rows" :key="row.label" class="recognized-item">
        <span>{{ row.label }}</span>
        <strong>{{ row.value }}</strong>
      </div>
    </div>
  </el-card>
</template>
