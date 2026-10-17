<script setup>
import { useRouter } from "vue-router";
import { admissionLevel, admissionDisplayText, admissionResearchRoute } from "../utils/recommendation";
import { computed } from "vue";
import GkSchoolLogo from "./GkSchoolLogo.vue";
import { normalizeItem, recommendationBasisLabel } from "../utils/recommendation";
import { rankOfScore as modelRankOfScore, scoreOfRank } from "../utils/scoreModel";

const researchRouter = useRouter();
const props = defineProps({
  item: { type: Object, required: true },
  strategy: { type: String, default: "safe" },
  added: { type: Boolean, default: false },
  showAddAction: { type: Boolean, default: false },
  userScore: { type: Number, default: null },
  userRank: { type: Number, default: null },
  researchConditions: { type: Object, default: () => ({}) }
});

const emit = defineEmits(["add", "view-detail", "pick-majors"]);

const model = computed(() => normalizeItem(props.item, props.strategy));

/* ===== 派生元数据（全部来自推荐项真实字段） ===== */
const metaLine = computed(() => {
  const s = model.value;
  const parts = [
    s.universityProvince || "",
    s.universityTier || "",
    ...(s.schoolTags || [])
  ].filter(Boolean);
  return parts.join(" · ");
});

/* ===== 概率徽章 / 推荐指数 ===== */
const probability = computed(() => {
  if (model.value.admissionProbability != null) return Number(model.value.admissionProbability);
  return null;
});
const probabilityText = computed(() => (probability.value == null ? "待测" : admissionLevel(probability.value)));
const tier = computed(() => {
  const t = model.value.strategy;
  if (t === "rush") return { key: "rush", label: "冲" };
  if (t === "guarantee") return { key: "guard", label: "保" };
  return { key: "safe", label: "稳" };
});
const stars = computed(() => {
  const p = probability.value;
  if (p >= 75) return 5;
  if (p >= 60) return 4.5;
  if (p >= 45) return 4;
  if (p >= 30) return 3.5;
  if (p >= 15) return 3;
  return 2.5;
});

/* ===== 三年录取数据（26 年为后端真实数据，24/25 确定性外推） ===== */
/* 【修复】原公式 `(720 - score) * 240` 是线性拍脑袋值（600 分 → 28800 名，
   与其他页面完全不一致），现在统一用 scoreModel 的一分一段模型 */
const rankOfScore = (score) => modelRankOfScore(score);
const resolvedUserScore = computed(() => {
  if (props.userScore != null) return Number(props.userScore);
  const m = model.value;
  if (m.cutoffScore != null && m.scoreGap != null) return Number(m.cutoffScore) - Number(m.scoreGap);
  if (props.userRank != null) return scoreOfRank(Number(props.userRank));
  if (m.userRank != null) return scoreOfRank(Number(m.userRank));
  return null;
});
/* 历年数据：仅展示推荐项自带的当年真实录取数据，不再编造往年数字 */
const years = computed(() => {
  const m = model.value;
  if (m.cutoffScore == null && m.minRank == null) return [];
  const score = m.cutoffScore != null ? Number(m.cutoffScore) : null;
  const minRank = m.minRank != null ? Number(m.minRank) : null;
  const eq = resolvedUserScore.value != null && score != null ? score : null;
  const diff = eq != null && resolvedUserScore.value != null ? eq - resolvedUserScore.value : null;
  return [{
    year: m.admissionYear || "最新",
    plan: m.planCount != null ? Number(m.planCount) : null,
    score,
    minRank,
    eq,
    diff
  }];
});
const rankCompare = computed(() => {
  const m = model.value;
  const user = props.userRank ?? m.userRank;
  const first = years.value[0];
  const min = first && first.minRank != null ? first.minRank : m.minRank != null ? Number(m.minRank) : null;
  if (user == null || min == null) return null;
  const lead = min - Number(user);
  if (lead > 0) return { text: `位次领先 ${lead.toLocaleString("zh-CN")} 名`, ahead: true };
  if (lead < 0) return { text: `位次落后 ${(-lead).toLocaleString("zh-CN")} 名`, ahead: false };
  return { text: "位次基本持平", ahead: null };
});
const basisLabel = computed(() => recommendationBasisLabel(model.value.recommendationBasis));
const isDirectAddMode = computed(() => model.value.recommendationMode === "MAJOR_FIRST" || !!model.value.majorName);
const actionLabel = computed(() => (isDirectAddMode.value ? (props.added ? "已加入" : "加入志愿表") : "加入志愿表"));
const logoSchool = computed(() => ({
  id: model.value.universityId,
  name: model.value.universityName
}));

function handleAdd() {
  emit("add", props.item, props.strategy);
}
function handleDetail() {
  emit("view-detail", props.item, props.strategy);
}
</script>

<template>
  <article class="mnz-rlrow" :class="`is-${tier.key}`">
    <div class="mnz-rlrow__top">
      <span class="mnz-rlrow__badge">
        <em>{{ probabilityText }}</em>
      </span>

      <div class="mnz-rlrow__identity" @click="handleDetail">
        <GkSchoolLogo :school="logoSchool" size="sm" class="mnz-rlrow__logo" />
        <div class="mnz-rlrow__titled">
          <h4 class="mnz-rlrow__name">
            {{ model.universityName }}
          </h4>
          <div class="mnz-rlrow__meta">
            <span v-if="model.majorName" class="mnz-rlrow__major">专业：{{ model.majorName }}</span>
            {{ metaLine }}
          </div>
        </div>
        <div class="mnz-rlrow__flags">
          <em v-if="model.is985">985</em>
          <!-- 双一流 ≡ 211：211 徽章取消，统一显示双一流（20260820 概念归并） -->
          <em v-if="model.is211 || model.isDoubleFirstClass">双一流</em>
        </div>
      </div>

      <div class="mnz-rlrow__side">
        <div class="mnz-rlrow__stars">
          <span>推荐指数</span>
          <el-rate :model-value="stars" disabled allow-half size="small" />
        </div>
        <div class="mnz-rlrow__acts" style="flex-wrap:wrap;gap:8px">
          <button type="button" class="mnz-rlrow__majors" @click="researchRouter.push(admissionResearchRoute(model.universityId, model.universityName, model.majorName, { ...researchConditions, ...(userScore != null ? { score: userScore } : {}), ...(userRank != null ? { userRank } : {}) }))">AI 录取分析</button>
          <button type="button" class="mnz-rlrow__majors" @click="emit('pick-majors', item, strategy)">可填专业</button>
          <button
            v-if="showAddAction"
            type="button"
            class="mnz-rlrow__add"
            :class="{ 'is-added': added && isDirectAddMode }"
            :disabled="added && isDirectAddMode"
            @click="handleAdd"
          >
            {{ actionLabel }}
          </button>
        </div>
      </div>
    </div>

    <div class="mnz-rlrow__data">
      <div class="mnz-rlrow__plan" v-if="years.length">
        <span>{{ years[0].year }}年计划</span>
        <strong>{{ years[0].plan == null ? "—" : years[0].plan + " 人" }}</strong>
      </div>

      <div class="mnz-rlrow__cols" v-if="years.length">
        <div v-for="y in years" :key="y.year" class="mnz-rlrow__col">
          <span class="mnz-rlrow__col-year">{{ y.year }}年</span>
          <div class="mnz-rlrow__cell">
            <label>最低分</label>
            <strong>{{ y.score }}</strong>
          </div>
          <div class="mnz-rlrow__cell">
            <label>最低位次</label>
            <strong>{{ y.minRank == null ? "待测" : y.minRank.toLocaleString("zh-CN") }}</strong>
          </div>
          <div class="mnz-rlrow__cell">
            <label>等效分</label>
            <strong>{{ y.eq }}</strong>
          </div>
          <div class="mnz-rlrow__cell">
            <label>分差</label>
            <strong
              class="mnz-rlrow__diff"
              :class="y.diff == null ? 'is-flat' : y.diff > 0 ? 'is-up' : y.diff < 0 ? 'is-down' : 'is-flat'"
            >
              {{ y.diff == null ? "-" : (y.diff > 0 ? "+" : "") + y.diff }}
              {{ y.diff > 0 ? "↑" : y.diff < 0 ? "↓" : "" }}
            </strong>
          </div>
        </div>
      </div>

      <div class="mnz-rlrow__facts">
        <span v-if="rankCompare" class="mnz-rlrow__rankcmp" :class="{ 'is-ahead': rankCompare.ahead === true, 'is-behind': rankCompare.ahead === false }">
          {{ rankCompare.text }}
        </span>
        <span class="mnz-rlrow__basis">{{ basisLabel }}</span>
      </div>
    </div>

    <p v-if="model.explanation" class="mnz-rlrow__reason">{{ model.explanation }}</p>
  </article>
</template>
