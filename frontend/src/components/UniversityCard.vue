<script setup>
import { computed } from "vue";
import { OfficeBuilding } from "@element-plus/icons-vue";
import { normalizeItem, recommendationBasisLabel } from "../utils/recommendation";

const props = defineProps({
  item: { type: Object, required: true },
  strategy: { type: String, default: "safe" },
  added: { type: Boolean, default: false },
  showAddAction: { type: Boolean, default: false }
});

const emit = defineEmits(["add", "view-detail"]);
const model = computed(() => normalizeItem(props.item, props.strategy));
const resolvedBasisLabel = computed(() => recommendationBasisLabel(model.value.recommendationBasis));
const actionLabel = computed(() => "查看专业组");
const showRankMetric = computed(() =>
  model.value.recommendationBasis === "RANK"
  || model.value.minRank != null
  || model.value.userRank != null
);
const probabilityText = computed(() =>
  model.value.admissionProbability == null ? "-" : `${model.value.admissionProbability}%`
);
const riskText = computed(() =>
  model.value.riskScore == null ? "-" : `${model.value.riskScore}/100`
);
const matchReasons = computed(() =>
  Array.isArray(model.value.matchReasons) ? model.value.matchReasons.filter(Boolean).slice(0, 3) : []
);
const schoolTags = computed(() => {
  const list = [];
  if (model.value.universityProvince) {
    list.push(model.value.universityProvince);
  }
  (Array.isArray(model.value.schoolTags) ? model.value.schoolTags : []).forEach((item) => list.push(item));
  if (model.value.universityTags) {
    String(model.value.universityTags)
      .split(/[、,，\s]+/)
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => list.push(item));
  }
  return Array.from(new Set(list.filter((item) => item !== "普通"))).slice(0, 4);
});
const primaryGapLabel = computed(() => showRankMetric.value ? "位次差" : "分差");
const primaryGapValue = computed(() => showRankMetric.value ? model.value.rankGap : model.value.scoreGap);
const cutoffLabel = computed(() => {
  if (showRankMetric.value) {
    return "最低位次";
  }
  return model.value.majorName ? "专业最低分" : "院校最低分";
});
const cutoffValue = computed(() => showRankMetric.value ? model.value.minRank : model.value.cutoffScore);

function handleAction() {
  emit("view-detail", props.item, props.strategy);
}
</script>

<template>
  <article class="university-card">
    <header class="university-card__head">
      <div class="university-card__identity">
        <span class="university-card__mark"><el-icon><OfficeBuilding /></el-icon></span>
        <div class="university-card__title">
          <h4 class="university-card__name">{{ model.universityName }}</h4>
          <div v-if="model.majorName" class="university-card__major">
            <span v-if="model.professionalGroupCode" class="university-card__group-code">[{{ model.professionalGroupCode }}]</span>
            {{ model.majorName }}
          </div>
        </div>
      </div>
      <el-button
        v-if="showAddAction"
        class="university-card__action"
        type="primary"
        plain
        @click="handleAction"
      >
        {{ actionLabel }}
      </el-button>
    </header>

    <div v-if="schoolTags.length" class="university-card__tags">
      <el-tag v-for="tag in schoolTags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
    </div>

    <div class="university-card__content">
      <div class="university-card__metrics">
        <div class="university-card__metric">
          <span>录取概率</span>
          <strong class="university-card__probability">{{ probabilityText }}</strong>
        </div>
        <div class="university-card__metric">
          <span>风险指数</span>
          <strong>{{ riskText }}</strong>
        </div>
        <div class="university-card__metric">
          <span>{{ cutoffLabel }}</span>
          <strong>{{ cutoffValue ?? "-" }}</strong>
        </div>
        <div class="university-card__metric">
          <span>{{ primaryGapLabel }}</span>
          <strong>{{ primaryGapValue ?? "-" }}</strong>
        </div>
      </div>

      <div class="university-card__detail">
        <div class="university-card__basis">
          <span>判断依据</span>
          <strong>{{ resolvedBasisLabel }}</strong>
        </div>
        <div v-if="matchReasons.length" class="university-card__reasons">
          <span>匹配理由</span>
          <ul>
            <li v-for="(reason, index) in matchReasons" :key="`${model.universityName}-${index}`">{{ reason }}</li>
          </ul>
        </div>
        <p v-if="model.explanation" class="university-card__explanation">{{ model.explanation }}</p>
      </div>
    </div>
  </article>
</template>

<style scoped>
.university-card__group-code {
  margin-right: 4px;
  color: #ff5a36;
  font-weight: 700;
}
</style>
