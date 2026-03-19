<script setup>
import { computed } from "vue";
import { normalizeItem, strategyLabel, strategyTagType } from "../utils/recommendation";

const props = defineProps({
  item: { type: Object, required: true },
  strategy: { type: String, default: "safe" },
  added: { type: Boolean, default: false },
  showAddAction: { type: Boolean, default: false }
});

const emit = defineEmits(["add"]);
const model = computed(() => normalizeItem(props.item, props.strategy));
const resolvedStrategyLabel = computed(() => strategyLabel(model.value.strategy));
const resolvedStrategyType = computed(() => strategyTagType(model.value.strategy));
const showRankMetric = computed(() =>
  model.value.recommendationBasis === "RANK"
  || model.value.minRank != null
  || model.value.userRank != null
);
const showScoreMetric = computed(() => !showRankMetric.value && model.value.cutoffScore != null);
const schoolTags = computed(() => {
  const list = [];
  if (model.value.universityProvince) {
    list.push(model.value.universityProvince);
  }
  if (model.value.universityTier) {
    list.push(model.value.universityTier);
  }
  if (model.value.universityTags) {
    String(model.value.universityTags)
      .split(/[、,，\s]+/)
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => list.push(item));
  }
  return Array.from(new Set(list)).slice(0, 3);
});
</script>

<template>
  <el-card class="university-card" shadow="hover">
    <div class="university-card__head">
      <div class="university-card__title">
        <h4 class="university-card__name">{{ model.universityName }}</h4>
        <div v-if="model.majorName" class="university-card__major">{{ model.majorName }}</div>
      </div>
      <el-tag size="small" :type="resolvedStrategyType" effect="light">{{ resolvedStrategyLabel }}</el-tag>
    </div>
    <div v-if="schoolTags.length" class="university-card__tags">
      <el-tag v-for="tag in schoolTags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
    </div>
    <div class="university-card__meta">
      <div v-if="showRankMetric" class="meta-row"><span>最低位次</span><strong>{{ model.minRank ?? "-" }}</strong></div>
      <div v-if="showRankMetric" class="meta-row"><span>位次差</span><strong>{{ model.rankGap ?? "-" }}</strong></div>
      <div v-if="showScoreMetric" class="meta-row"><span>{{ model.majorName ? "专业最低分" : "院校最低分" }}</span><strong>{{ model.cutoffScore ?? "-" }}</strong></div>
      <div v-if="showScoreMetric" class="meta-row"><span>分差</span><strong>{{ model.scoreGap ?? "-" }}</strong></div>
    </div>
    <div v-if="showAddAction" class="university-card__footer">
      <el-button type="primary" plain :disabled="added" @click="emit('add', item, strategy)">
        {{ added ? "已加入" : "加入方案" }}
      </el-button>
    </div>
  </el-card>
</template>
