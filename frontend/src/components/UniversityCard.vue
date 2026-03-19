<script setup>
import { computed } from "vue";
import { normalizeItem } from "../utils/recommendation";

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  strategy: {
    type: String,
    default: "safe"
  }
});

const model = computed(() => normalizeItem(props.item, props.strategy));

const strategyLabel = computed(() => {
  if (model.value.strategy === "rush") return "冲刺";
  if (model.value.strategy === "guarantee") return "保底";
  return "稳妥";
});

const strategyType = computed(() => {
  if (model.value.strategy === "rush") return "danger";
  if (model.value.strategy === "guarantee") return "success";
  return "warning";
});

const probabilityText = computed(() =>
  model.value.admissionProbability === null ? "-" : `${model.value.admissionProbability}%`
);
</script>

<template>
  <el-card class="university-card" shadow="hover">
    <div class="university-card__head">
      <h4 class="university-card__name">{{ model.universityName }}</h4>
      <el-tag size="small" :type="strategyType" effect="light">{{ strategyLabel }}</el-tag>
    </div>
    <div class="university-card__meta">
      <div class="meta-row"><span>录取线</span><strong>{{ model.cutoffScore ?? "-" }}</strong></div>
      <div class="meta-row"><span>分差</span><strong>{{ model.scoreGap ?? "-" }}</strong></div>
      <div class="meta-row"><span>录取概率</span><strong>{{ probabilityText }}</strong></div>
    </div>
  </el-card>
</template>
