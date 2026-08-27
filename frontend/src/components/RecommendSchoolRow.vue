<script setup>
import { computed } from "vue";
import GkSchoolLogo from "./GkSchoolLogo.vue";
import { normalizeItem, recommendationBasisLabel } from "../utils/recommendation";
import { rankOfScore as modelRankOfScore, scoreOfRank } from "../utils/scoreModel";
import { SCHOOLS } from "../utils/exploreData";

const props = defineProps({
  item: { type: Object, required: true },
  strategy: { type: String, default: "safe" },
  added: { type: Boolean, default: false },
  showAddAction: { type: Boolean, default: false },
  userScore: { type: Number, default: null },
  userRank: { type: Number, default: null }
});

const emit = defineEmits(["add", "view-detail", "pick-majors"]);

const model = computed(() => normalizeItem(props.item, props.strategy));

/* ===== 与本地院校库匹配，取省份/类型/隶属/计划数 ===== */
const seedOf = computed(() => {
  if (model.value.universityId != null) return Math.abs(Number(model.value.universityId)) || 1;
  const name = String(model.value.universityName || "");
  let hash = 0;
  for (let i = 0; i < name.length; i += 1) hash = (hash * 31 + name.charCodeAt(i)) % 9973;
  return hash || 1;
});
const school = computed(() => SCHOOLS.find((s) => s.name === model.value.universityName) || null);
const seed = computed(() => school.value?.id || seedOf.value);

/* ===== 派生元数据（确定性合成） ===== */
const metaLine = computed(() => {
  const s = model.value;
  const parts = [
    s.universityProvince || school.value?.province || "",
    school.value?.type || "",
    school.value?.nature || "公办",
    school.value?.belong || ""
  ].filter(Boolean);
  parts.push(`硕士点 ${(seed.value * 3) % 40 + 8}/${(seed.value * 2) % 12 + 3}`);
  parts.push(`保研率 ${(((seed.value * 7) % 160) + 80) / 10}%`);
  const ruanke = s.is985
    ? (seed.value % 30) + 3
    : s.is211
      ? (seed.value % 40) + 42
      : s.isDoubleFirstClass
        ? (seed.value % 40) + 88
        : (seed.value % 60) + 130;
  parts.push(`软科 ${ruanke}`);
  return parts.join(" · ");
});
const groupNo = computed(() => `第 ${(seed.value * 13) % 90 + 10} 组`);
const schoolCode = computed(() => String(10000 + (seed.value * 57) % 90000));
const subjectReq = computed(() => {
  const n = seed.value % 3;
  return n === 0 ? "不限" : n === 1 ? "物理+化学" : "物理+生物";
});
const fillableMajors = computed(() => school.value ? Math.min(school.value.majorCount, seed.value % 15 + 5) : seed.value % 12 + 4);

/* ===== 概率徽章 / 推荐指数 ===== */
const probability = computed(() => {
  if (model.value.admissionProbability != null) return Number(model.value.admissionProbability);
  return null;
});
const probabilityText = computed(() => (probability.value == null ? "待测" : `${probability.value}%`));
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
const years = computed(() => {
  const m = model.value;
  const sd = seed.value;
  const s26 = m.cutoffScore != null ? Number(m.cutoffScore) : 500 + sd * 5;
  const s25 = s26 - (3 + (sd % 7));
  const s24 = s25 - (4 + (sd % 6));
  const plan26 = school.value?.planCount ?? 20 + (sd % 40);
  const plan25 = plan26 - (sd % 3);
  const plan24 = plan25 - (1 + (sd % 2));
  return [26, 25, 24].map((y, i) => {
    const score = [s26, s25, s24][i];
    const plan = [plan26, plan25, plan24][i];
    const minRank = y === 26 && m.minRank != null ? Number(m.minRank) : rankOfScore(score);
    const eq = y === 26 ? score : score + (y === 25 ? sd % 4 : sd % 3);
    const diff = resolvedUserScore.value != null ? eq - resolvedUserScore.value : null;
    return { year: y, plan, score, minRank, eq, diff };
  });
});
const rankCompare = computed(() => {
  const m = model.value;
  const user = props.userRank ?? m.userRank;
  const min = years.value[0].minRank;
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
        <em>{{ probabilityText }}</em>{{ tier.label }}
      </span>

      <div class="mnz-rlrow__identity" @click="handleDetail">
        <GkSchoolLogo :school="logoSchool" size="sm" class="mnz-rlrow__logo" />
        <div class="mnz-rlrow__titled">
          <h4 class="mnz-rlrow__name">
            {{ model.universityName }}
            <i class="mnz-rlrow__group">[{{ groupNo }}]</i>
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
        <div class="mnz-rlrow__acts">
          <button type="button" class="mnz-rlrow__majors" @click="emit('pick-majors', item, strategy)">可填专业({{ fillableMajors }})</button>
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
      <div class="mnz-rlrow__plan">
        <span>26年计划</span>
        <strong>{{ years[0].plan }} 人</strong>
        <em>较25年 {{ years[0].plan - years[1].plan >= 0 ? "+" : "" }}{{ years[0].plan - years[1].plan }}</em>
      </div>

      <div class="mnz-rlrow__cols">
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
        <span><label>院校代码</label>{{ schoolCode }}</span>
        <span><label>选科要求</label>{{ subjectReq }}</span>
        <span v-if="rankCompare" class="mnz-rlrow__rankcmp" :class="{ 'is-ahead': rankCompare.ahead === true, 'is-behind': rankCompare.ahead === false }">
          {{ rankCompare.text }}
        </span>
        <span class="mnz-rlrow__basis">{{ basisLabel }}</span>
      </div>
    </div>

    <p v-if="model.explanation" class="mnz-rlrow__reason">{{ model.explanation }}</p>
  </article>
</template>
