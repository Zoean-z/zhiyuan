<script setup>
import { computed, ref, watch } from "vue";
import { Search } from "@element-plus/icons-vue";
import AiSummaryPanel from "./AiSummaryPanel.vue";
import RecommendSchoolRow from "./RecommendSchoolRow.vue";
import { buildPlanItemKey, normalizeItem, normalizeSchoolTags, pickValue } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";

const props = defineProps({
  loading: { type: Boolean, default: false },
  grouped: { type: Object, required: true },
  aiSummary: { type: String, default: "" },
  summary: { type: String, default: "" },
  finalAdvice: { type: String, default: "" },
  tips: { type: Array, default: () => [] },
  recommendationMode: { type: String, default: "" },
  rankMeta: { type: Object, default: null },
  showAddAction: { type: Boolean, default: false },
  showAiSummary: { type: Boolean, default: false },
  selectedPlanKeys: { type: Array, default: () => [] }
});

const emit = defineEmits(["add-item", "view-school-detail", "pick-majors"]);
const activeTab = ref("rush");
const rushList = computed(() => (Array.isArray(props.grouped?.rush) ? props.grouped.rush : []));
const safeList = computed(() => (Array.isArray(props.grouped?.safe) ? props.grouped.safe : []));
const guaranteeList = computed(() => (Array.isArray(props.grouped?.guarantee) ? props.grouped.guarantee : []));

/* ===== 筛选 / 排序 / 搜索（参考 mnzy 结果工具栏） ===== */
const TYPE_FILTERS = [
  { value: "ALL", label: "类型不限" },
  { value: "985", label: "985" },
  { value: "DOUBLE", label: "双一流" }
];
const SORTERS = [
  { value: "DEFAULT", label: "综合排序" },
  { value: "PROB", label: "概率从高到低" },
  { value: "DIFF", label: "分差从低到高" },
  { value: "RANK", label: "位次从高到低" }
];
const typeFilter = ref("ALL");
const sortKey = ref("DEFAULT");
const keyword = ref("");

const userScore = computed(() => {
  const score = props.rankMeta?.score;
  if (score != null && score !== "") return Number(score);
  const first = rushList.value[0] || safeList.value[0] || guaranteeList.value[0];
  const s = normalizeItem(first || {});
  if (s.cutoffScore != null && s.scoreGap != null) return Number(s.cutoffScore) - Number(s.scoreGap);
  return null;
});
const userRank = computed(() => {
  const meta = props.rankMeta?.userRank;
  if (meta != null) return Number(meta);
  const first = rushList.value[0] || safeList.value[0] || guaranteeList.value[0];
  return pickValue(first || {}, ["userRank"]) ?? null;
});

function matchTypeFilter(item) {
  if (typeFilter.value === "ALL") return true;
  const flags = normalizeSchoolTags(item);
  if (typeFilter.value === "985") return flags.is985;
  // 双一流 ≡ 211：选双一流时 211 院校同样命中（20260820 概念归并）
  return flags.is211 || flags.isDoubleFirstClass;
}
function matchKeyword(item) {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return true;
  const name = String(pickValue(item, ["universityName", "schoolName", "name"]) || "").toLowerCase();
  const major = String(pickValue(item, ["majorName", "major"]) || "").toLowerCase();
  return name.includes(kw) || major.includes(kw);
}
function sortList(list) {
  if (sortKey.value === "DEFAULT") return list;
  const sorted = [...list];
  const probOf = (item) => {
    const p = pickValue(item, ["admissionProbability", "probability", "chance"]);
    if (p != null) return Number(p);
    const risk = pickValue(item, ["riskScore"]);
    return risk != null ? 100 - Number(risk) : 50;
  };
  const cutoffOf = (item) => {
    const c = pickValue(item, ["cutoffScore", "cutoff", "lastYearCutoff"]);
    return c != null ? Number(c) : Number.MAX_SAFE_INTEGER;
  };
  const rankOf = (item) => {
    const r = pickValue(item, ["minRank", "minimumRank"]);
    return r != null ? Number(r) : -Number.MAX_SAFE_INTEGER;
  };
  if (sortKey.value === "PROB") sorted.sort((a, b) => probOf(b) - probOf(a));
  if (sortKey.value === "DIFF") sorted.sort((a, b) => cutoffOf(a) - cutoffOf(b));
  if (sortKey.value === "RANK") sorted.sort((a, b) => rankOf(b) - rankOf(a));
  return sorted;
}
function processList(list) {
  return sortList(list.filter((item) => matchTypeFilter(item) && matchKeyword(item)));
}
const filteredRush = computed(() => processList(rushList.value));
const filteredSafe = computed(() => processList(safeList.value));
const filteredGuarantee = computed(() => processList(guaranteeList.value));
const totalCount = computed(() => rushList.value.length + safeList.value.length + guaranteeList.value.length);

const hasAnyRankFields = computed(() => {
  const firstItem = rushList.value[0] || safeList.value[0] || guaranteeList.value[0] || null;
  return !!firstItem && (
    firstItem.userRank != null
    || firstItem.minRank != null
    || firstItem.rankGap != null
    || !!firstItem.recommendationBasis
  );
});
const resolvedRankMeta = computed(() => {
  const meta = props.rankMeta || {};
  const firstItem = rushList.value[0] || safeList.value[0] || guaranteeList.value[0] || {};
  return {
    score: meta.score ?? null,
    province: meta.province ?? "",
    subjectTypeLabel: meta.subjectTypeLabel ?? "",
    userRank: meta.userRank ?? firstItem.userRank ?? null,
    recommendationBasis: meta.recommendationBasis ?? firstItem.recommendationBasis ?? ""
  };
});
const showRankPanel = computed(() =>
  !!props.rankMeta
  || hasAnyRankFields.value
  || resolvedRankMeta.value.userRank != null
);
const resolvedRecommendationMode = computed(() => {
  if (props.recommendationMode) {
    return props.recommendationMode;
  }
  const firstItem = rushList.value[0] || safeList.value[0] || guaranteeList.value[0] || null;
  return firstItem?.recommendationMode || "SCHOOL_FIRST";
});
const resultTargetText = computed(() => resolvedRecommendationMode.value === "MAJOR_FIRST" ? "学校+专业" : "院校");
const selectedKeySet = computed(() => new Set(props.selectedPlanKeys));

watch(
  [rushList, safeList, guaranteeList],
  ([rush, safe, guarantee]) => {
    const lists = { rush, safe, guarantee };
    if (lists[activeTab.value]?.length) return;
    activeTab.value = ["rush", "safe", "guarantee"].find((key) => lists[key].length) || "rush";
  },
  { immediate: true }
);

function isItemAdded(item, strategy) {
  return selectedKeySet.value.has(buildPlanItemKey(item, strategy));
}
</script>

<template>
  <section class="result-page">
    <div class="result-main">
      <el-skeleton :loading="loading" animated>
        <template #template>
          <div class="mnz-rlist">
            <el-skeleton-item v-for="index in 3" :key="index" variant="p" class="recommend-card-skeleton" />
          </div>
        </template>

        <template #default>
          <div v-if="totalCount" class="mnz-rlist__toolbar">
            <div class="mnz-rlist__total">
              共 <strong>{{ totalCount }}</strong> 所院校
              <em class="mnz-rlist__chip is-rush">冲刺 {{ rushList.length }}</em>
              <em class="mnz-rlist__chip is-safe">稳妥 {{ safeList.length }}</em>
              <em class="mnz-rlist__chip is-guard">保底 {{ guaranteeList.length }}</em>
            </div>
            <div class="mnz-rlist__filters">
              <el-select v-model="typeFilter" class="mnz-rlist__filter" size="small">
                <el-option v-for="opt in TYPE_FILTERS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-select v-model="sortKey" class="mnz-rlist__filter" size="small">
                <el-option v-for="opt in SORTERS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-input
                v-model="keyword"
                class="mnz-rlist__search"
                size="small"
                clearable
                placeholder="输入院校名称"
                :prefix-icon="Search"
              />
            </div>
          </div>

          <el-tabs v-model="activeTab" class="recommend-tabs">
            <el-tab-pane :label="'冲刺 ' + filteredRush.length" name="rush">
              <div v-if="filteredRush.length" class="mnz-rlist">
                <RecommendSchoolRow
                  v-for="(item, idx) in filteredRush"
                  :key="'rush-' + idx"
                  :item="item"
                  strategy="rush"
                  :show-add-action="showAddAction"
                  :added="isItemAdded(item, 'rush')"
                  :user-score="userScore"
                  :user-rank="userRank"
                  @add="emit('add-item', item, 'rush')"
                  @view-detail="emit('view-school-detail', item, 'rush')"
                  @pick-majors="emit('pick-majors', item, 'rush')"
                />
              </div>
              <el-empty
                v-else
                :description="rushList.length ? '当前筛选条件下没有匹配院校' : UI_TEXT.empty.noRush + resultTargetText"
                :image-size="90"
              />
            </el-tab-pane>

            <el-tab-pane :label="'稳妥 ' + filteredSafe.length" name="safe">
              <div v-if="filteredSafe.length" class="mnz-rlist">
                <RecommendSchoolRow
                  v-for="(item, idx) in filteredSafe"
                  :key="'safe-' + idx"
                  :item="item"
                  strategy="safe"
                  :show-add-action="showAddAction"
                  :added="isItemAdded(item, 'safe')"
                  :user-score="userScore"
                  :user-rank="userRank"
                  @add="emit('add-item', item, 'safe')"
                  @view-detail="emit('view-school-detail', item, 'safe')"
                  @pick-majors="emit('pick-majors', item, 'safe')"
                />
              </div>
              <el-empty
                v-else
                :description="safeList.length ? '当前筛选条件下没有匹配院校' : UI_TEXT.empty.noSafe + resultTargetText"
                :image-size="90"
              />
            </el-tab-pane>

            <el-tab-pane :label="'保底 ' + filteredGuarantee.length" name="guarantee">
              <div v-if="filteredGuarantee.length" class="mnz-rlist">
                <RecommendSchoolRow
                  v-for="(item, idx) in filteredGuarantee"
                  :key="'guarantee-' + idx"
                  :item="item"
                  strategy="guarantee"
                  :show-add-action="showAddAction"
                  :added="isItemAdded(item, 'guarantee')"
                  :user-score="userScore"
                  :user-rank="userRank"
                  @add="emit('add-item', item, 'guarantee')"
                  @view-detail="emit('view-school-detail', item, 'guarantee')"
                  @pick-majors="emit('pick-majors', item, 'guarantee')"
                />
              </div>
              <el-empty
                v-else
                :description="guaranteeList.length ? '当前筛选条件下没有匹配院校' : UI_TEXT.empty.noGuarantee + resultTargetText"
                :image-size="90"
              />
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-skeleton>
    </div>

    <AiSummaryPanel
      v-if="showAiSummary && (aiSummary || summary || finalAdvice || tips.length)"
      :ai-summary="aiSummary"
      :summary="summary"
      :final-advice="finalAdvice"
      :tips="tips"
    />
  </section>
</template>
