<script setup>
import { computed } from "vue";
import { normalizeItem } from "../utils/recommendation";

const props = defineProps({
  item: { type: Object, required: true },
  strategy: { type: String, default: "safe" }
});

const model = computed(() => normalizeItem(props.item, props.strategy));
const strategyLabel = computed(() => model.value.strategy === "rush" ? "冲刺" : model.value.strategy === "guarantee" ? "保底" : "稳妥");
const strategyType = computed(() => model.value.strategy === "rush" ? "danger" : model.value.strategy === "guarantee" ? "success" : "warning");
const probabilityText = computed(() => model.value.admissionProbability === null ? "-" : `${model.value.admissionProbability}%`);
</script>

<template>
  <el-card class="university-card" shadow="hover">
    <div class="university-card__head">
      <h4 class="university-card__name">{{ model.universityName }}</h4>
      <el-tag size="small" :type="strategyType" effect="light">{{ strategyLabel }}</el-tag>
    </div>
    <div class="university-card__meta">
      <div class="meta-row"><span>录取位次</span><strong>{{ model.minRank ?? "-" }}</strong></div>
      <div class="meta-row"><span>用户位次</span><strong>{{ model.userRank ?? "-" }}</strong></div>
      <div class="meta-row"><span>位次差</span><strong>{{ model.rankGap ?? "-" }}</strong></div>
      <div class="meta-row meta-row--subtle"><span>参考录取线</span><strong>{{ model.cutoffScore ?? "-" }}</strong></div>
      <div class="meta-row"><span>录取概率</span><strong>{{ probabilityText }}</strong></div>
    </div>
  </el-card>
</template>
