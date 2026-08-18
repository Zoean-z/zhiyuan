<script setup>
import { computed } from "vue";
import GkSchoolLogo from "./GkSchoolLogo.vue";
import { normalizeItem, recommendationBasisLabel } from "../utils/recommendation";

const props = defineProps({
  item: { type: Object, required: true },
  strategy: { type: String, default: "safe" },
  added: { type: Boolean, default: false },
  showAddAction: { type: Boolean, default: false }
});
const emit = defineEmits(["add", "view-detail"]);
const model = computed(() => normalizeItem(props.item, props.strategy));
const tier = computed(() => ({
  rush: { label: "冲刺", short: "冲" },
  safe: { label: "稳妥", short: "稳" },
  guarantee: { label: "保底", short: "保" }
}[model.value.strategy] || { label: "推荐", short: "荐" }));
const probabilityText = computed(() => model.value.admissionProbability == null ? "暂无" : `${model.value.admissionProbability}%`);
const schoolTags = computed(() => Array.from(new Set([
  model.value.universityProvince,
  ...(model.value.schoolTags || []),
  ...String(model.value.universityTags || "").split(/[、,，\s]+/)
].filter(Boolean))).slice(0, 5));
const reasons = computed(() => (model.value.matchReasons || []).filter(Boolean).slice(0, 3));
const directAdd = computed(() => model.value.recommendationMode === "MAJOR_FIRST");
const actionLabel = computed(() => directAdd.value ? (props.added ? "已加入" : "加入志愿单") : "查看专业组");
function handleAction() {
  if (directAdd.value) emit("add", props.item, props.strategy);
  else emit("view-detail", props.item, props.strategy);
}
</script>

<template>
  <article class="mnz-school-row" :class="`is-${model.strategy}`">
    <div class="mnz-school-row__badge"><strong>{{ probabilityText }}</strong><span>{{ tier.short }}</span></div>
    <GkSchoolLogo :school="{ id: model.universityId, name: model.universityName }" class="mnz-school-row__logo" />
    <div class="mnz-school-row__main">
      <div class="mnz-school-row__title-line">
        <div>
          <h3>{{ model.universityName }}</h3>
          <p v-if="model.majorName"><span v-if="model.professionalGroupCode">[{{ model.professionalGroupCode }}] </span>{{ model.majorName }}</p>
        </div>
        <div class="mnz-school-row__tags"><span v-for="tag in schoolTags" :key="tag">{{ tag }}</span></div>
      </div>
      <div class="mnz-school-row__metrics">
        <div><span>录取概率</span><strong class="is-probability">{{ probabilityText }}</strong></div>
        <div><span>最低分</span><strong>{{ model.cutoffScore ?? "-" }}</strong></div>
        <div><span>最低位次</span><strong>{{ model.minRank == null ? "-" : Number(model.minRank).toLocaleString("zh-CN") }}</strong></div>
        <div><span>分差 / 位次差</span><strong>{{ model.scoreGap ?? model.rankGap ?? "-" }}</strong></div>
      </div>
      <div class="mnz-school-row__basis">
        <span>{{ recommendationBasisLabel(model.recommendationBasis) }}</span>
        <span v-for="reason in reasons" :key="reason">{{ reason }}</span>
      </div>
      <p v-if="model.explanation" class="mnz-school-row__explanation">{{ model.explanation }}</p>
    </div>
    <button v-if="showAddAction" type="button" class="mnz-school-row__action"
      :disabled="directAdd && added" @click="handleAction">{{ actionLabel }}</button>
  </article>
</template>
