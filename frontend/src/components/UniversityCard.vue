<script setup>
import { computed } from "vue";
import { normalizeItem, recommendationBasisLabel, strategyTagType } from "../utils/recommendation";

const props = defineProps({
  item: { type: Object, required: true },
  strategy: { type: String, default: "safe" },
  added: { type: Boolean, default: false },
  showAddAction: { type: Boolean, default: false }
});

const emit = defineEmits(["add", "view-detail"]);
const model = computed(() => normalizeItem(props.item, props.strategy));
const resolvedStrategyLabel = computed(() => model.value.strategyLabel || "稳妥");
const resolvedStrategyType = computed(() => strategyTagType(model.value.strategy));
const resolvedBasisLabel = computed(() => recommendationBasisLabel(model.value.recommendationBasis));
const isDirectAddMode = computed(() => model.value.recommendationMode === "MAJOR_FIRST" || !!model.value.majorName);
const actionLabel = computed(() => {
  if (!isDirectAddMode.value) {
    return "查看专业";
  }
  return props.added ? "已加入" : "加入方案";
});
const actionDisabled = computed(() => isDirectAddMode.value && props.added);
const showRankMetric = computed(() =>
  model.value.recommendationBasis === "RANK"
  || model.value.minRank != null
  || model.value.userRank != null
);
const showScoreMetric = computed(() => !showRankMetric.value && model.value.cutoffScore != null);
const probabilityText = computed(() =>
  model.value.admissionProbability == null ? "-" : `${model.value.admissionProbability}%`
);
const riskText = computed(() =>
  model.value.riskScore == null ? "-" : `${model.value.riskScore}/100`
);
const matchReasons = computed(() =>
  Array.isArray(model.value.matchReasons) ? model.value.matchReasons.filter(Boolean).slice(0, 4) : []
);
const schoolTags = computed(() => {
  const list = [];
  if (model.value.universityProvince) {
    list.push(model.value.universityProvince);
  }
  (Array.isArray(model.value.schoolTags) ? model.value.schoolTags : []).forEach((item) => list.push(item));
  if (model.value.universityTags) {
    String(model.value.universityTags)
      .split(/[、,\s]+/)
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => list.push(item));
  }
  return Array.from(new Set(list.filter((item) => item !== "普通"))).slice(0, 5);
});

function handleAction() {
  if (isDirectAddMode.value) {
    emit("add", props.item, props.strategy);
    return;
  }
  emit("view-detail", props.item, props.strategy);
}
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
      <div class="meta-row"><span>判断依据</span><strong>{{ resolvedBasisLabel }}</strong></div>
      <div class="meta-row"><span>录取概率</span><strong>{{ probabilityText }}</strong></div>
      <div class="meta-row"><span>风险指数</span><strong>{{ riskText }}</strong></div>
      <div v-if="showRankMetric" class="meta-row"><span>最低位次</span><strong>{{ model.minRank ?? "-" }}</strong></div>
      <div v-if="showRankMetric" class="meta-row"><span>位次差</span><strong>{{ model.rankGap ?? "-" }}</strong></div>
      <div v-if="showScoreMetric" class="meta-row"><span>{{ model.majorName ? "专业最低分" : "院校最低分" }}</span><strong>{{ model.cutoffScore ?? "-" }}</strong></div>
      <div v-if="showScoreMetric" class="meta-row"><span>分差</span><strong>{{ model.scoreGap ?? "-" }}</strong></div>
    </div>
    <div v-if="matchReasons.length" class="university-card__reasons">
      <div class="university-card__section-title">匹配理由</div>
      <ul class="university-card__reason-list">
        <li v-for="(reason, index) in matchReasons" :key="`${model.universityName}-${index}`">{{ reason }}</li>
      </ul>
    </div>
    <div v-if="model.explanation" class="university-card__explanation">
      <div class="university-card__section-title">规则解释</div>
      <p>{{ model.explanation }}</p>
    </div>
    <div v-if="showAddAction" class="university-card__footer">
      <el-button type="primary" plain :disabled="actionDisabled" @click="handleAction">
        {{ actionLabel }}
      </el-button>
    </div>
  </el-card>
</template>
