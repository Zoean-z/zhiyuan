<script setup>
import { computed, inject, onMounted, onUnmounted, ref, watch } from "vue";
import { Delete } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import ApplicationPlanView from "../components/ApplicationPlanView.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import {
  buildPlanSchoolKey,
  flattenPlanItems,
  formatDateTime,
  groupPlanItemsBySchool,
  normalizeItem,
  strategyLabel
} from "../utils/recommendation";

const {
  auth, currentPlanItems, deletePlan, invalidatePlanLoad, loadCurrentPlanDraft, loadPlanIntoCurrent,
  loadPlans, openPlanDetail, persistCurrentPlanItems, planDetail, planDetailLoading,
  planDialogVisible, planLoading, planRecords, resetPlanDialog, saveDialogVisible,
  saveForm, savePlan, saveSubmitting
} = inject("workspace");

const displayMode = ref("detail");
const editableItems = ref([]);
const draftSaving = ref(false);

const schoolGroups = computed(() => groupPlanItemsBySchool(editableItems.value));
const groupedByStrategy = computed(() => ({
  rush: schoolGroups.value.filter((group) => group.strategy === "rush"),
  safe: schoolGroups.value.filter((group) => group.strategy === "safe"),
  guarantee: schoolGroups.value.filter((group) => group.strategy === "guarantee")
}));
const profile = computed(() => auth.value?.user || {});
const profileSubjects = computed(() => {
  const primary = profile.value.subjectType === "PHYSICS" ? "物理" : profile.value.subjectType === "HISTORY" ? "历史" : "";
  const elective = Array.isArray(profile.value.electiveSubjects) ? profile.value.electiveSubjects : [];
  const map = { CHEMISTRY: "化学", BIOLOGY: "生物", GEOGRAPHY: "地理", POLITICS: "政治" };
  return [primary, ...elective.map((item) => map[item] || item)].filter(Boolean).join("/");
});
const detailGroups = computed(() => groupPlanItemsBySchool(flattenPlanItems(planDetail.value?.resultJson)));

watch(currentPlanItems, (items) => {
  editableItems.value = (items || []).map((item) => ({ ...normalizeItem(item, item?.strategy) }));
}, { deep: true, immediate: true });

function setAdjustment(group, value) {
  editableItems.value = editableItems.value.map((item) => buildPlanSchoolKey(item) === group.schoolKey
    ? { ...item, obeyAdjustment: value }
    : item);
}

function removeSchool(group) {
  editableItems.value = editableItems.value.filter((item) => buildPlanSchoolKey(item) !== group.schoolKey);
}

async function saveDraft() {
  draftSaving.value = true;
  try {
    await persistCurrentPlanItems(editableItems.value);
    ElMessage.success("当前志愿表已保存");
  } catch (error) {
    ElMessage.error(error?.message || "保存当前志愿表失败");
  } finally {
    draftSaving.value = false;
  }
}

async function loadHistoricalPlan(row) {
  try {
    await loadPlanIntoCurrent(row);
    ElMessage.success(`已将《${row.planName}》载入当前志愿表`);
  } catch (error) {
    ElMessage.error(error?.message || "载入方案失败");
  }
}

async function openHistoricalPlan(row) {
  await openPlanDetail(row);
}

onMounted(async () => {
  await Promise.all([loadCurrentPlanDraft(), loadPlans()]);
});

onUnmounted(() => {
  invalidatePlanLoad();
  planDialogVisible.value = false;
  resetPlanDialog();
});
</script>

<template>
  <el-main class="app-main plans-workspace">
    <section class="plan-overview">
      <div class="plan-overview__profile">
        <strong>{{ profile.examProvince || "未设置省份" }}</strong>
        <span>{{ profileSubjects || "未设置选科" }}</span>
        <b>{{ profile.score == null ? "未设置分数" : `${profile.score} 分` }}</b>
        <em>本科批</em>
      </div>
      <div class="plan-overview__progress">
        <span>已选 {{ schoolGroups.length }} 所学校</span>
        <i><b :style="{ width: `${Math.min(100, schoolGroups.length / 45 * 100)}%` }" /></i>
        <strong>{{ editableItems.length }} 个专业志愿</strong>
      </div>
      <div class="plan-overview__toolbar">
        <div class="plan-mode-switch" role="tablist" aria-label="志愿表显示模式">
          <button :class="{ 'is-active': displayMode === 'detail' }" @click="displayMode = 'detail'">详细模式</button>
          <button :class="{ 'is-active': displayMode === 'table' }" @click="displayMode = 'table'">表格模式</button>
        </div>
        <el-button type="primary" :loading="draftSaving" @click="saveDraft">保存当前表</el-button>
        <el-button type="primary" plain :disabled="!editableItems.length" @click="saveDialogVisible = true">另存为方案</el-button>
      </div>
    </section>

    <div class="plan-strategy-summary">
      <span class="is-rush">冲 {{ groupedByStrategy.rush.length }}</span>
      <span class="is-safe">稳 {{ groupedByStrategy.safe.length }}</span>
      <span class="is-guarantee">保 {{ groupedByStrategy.guarantee.length }}</span>
      <small>同一学校的专业已合并显示；服从调剂设置会同步到该校全部专业</small>
    </div>

    <section v-if="displayMode === 'detail'" class="plan-detail-mode">
      <article v-for="strategy in ['rush', 'safe', 'guarantee']" :key="strategy" class="plan-strategy-block">
        <header>
          <h2>{{ strategyLabel(strategy) }}志愿</h2>
          <span>{{ groupedByStrategy[strategy].length }} 所院校</span>
        </header>
        <div v-for="(group, index) in groupedByStrategy[strategy]" :key="group.schoolKey" class="plan-school-row">
          <i>{{ index + 1 }}</i>
          <div class="plan-school-row__school">
            <GkSchoolLogo :school="{ id: group.universityId, name: group.universityName }" size="mini" />
            <strong>{{ group.universityName }}</strong>
          </div>
          <div class="plan-school-row__majors">
            <span v-for="major in group.majors" :key="major">{{ major }}</span>
            <em v-if="!group.majors.length">院校志愿</em>
          </div>
          <button class="adjustment-toggle" :class="{ 'is-on': group.obeyAdjustment }" @click="setAdjustment(group, !group.obeyAdjustment)">
            {{ group.obeyAdjustment ? "服从调剂" : "不服从" }}
          </button>
          <span class="plan-probability" :class="`is-${strategy}`">
            {{ strategyLabel(strategy) }} {{ group.admissionProbability == null ? "-" : `${group.admissionProbability}%` }}
          </span>
          <el-button text circle :icon="Delete" aria-label="删除该校志愿" @click="removeSchool(group)" />
        </div>
        <el-empty v-if="!groupedByStrategy[strategy].length" description="该档位暂无志愿" :image-size="55" />
      </article>
    </section>

    <section v-else class="plan-table-wrap">
      <table class="plan-table">
        <thead><tr><th>序号</th><th>院校名称</th><th>录取档位</th><th>专业</th><th>服从调剂</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="(group, index) in schoolGroups" :key="group.schoolKey">
            <td>{{ String(index + 1).padStart(2, "0") }}</td>
            <td><GkSchoolLogo :school="{ id: group.universityId, name: group.universityName }" size="mini" /><strong>{{ group.universityName }}</strong></td>
            <td><span class="plan-probability" :class="`is-${group.strategy}`">{{ strategyLabel(group.strategy) }} {{ group.admissionProbability == null ? "-" : `${group.admissionProbability}%` }}</span></td>
            <td>{{ group.majors.join("、") || "院校志愿" }}</td>
            <td><button class="adjustment-text" @click="setAdjustment(group, !group.obeyAdjustment)">{{ group.obeyAdjustment ? "是" : "否" }}</button></td>
            <td><el-button text circle :icon="Delete" aria-label="删除该校志愿" @click="removeSchool(group)" /></td>
          </tr>
          <tr v-if="!schoolGroups.length"><td colspan="6"><el-empty description="还没有志愿，请从推荐结果或专业组中加入" :image-size="70" /></td></tr>
        </tbody>
      </table>
    </section>

    <ApplicationPlanView
      :records="planRecords"
      :loading="planLoading"
      @refresh="loadPlans"
      @view="openHistoricalPlan"
      @load="loadHistoricalPlan"
      @delete="deletePlan"
    />

    <el-dialog v-model="saveDialogVisible" title="保存填报方案" width="430px">
      <el-form label-position="top">
        <el-form-item label="方案名称"><el-input v-model="saveForm.planName" maxlength="30" placeholder="例如：湖南物理类正式方案" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveSubmitting" @click="savePlan">保存方案</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="planDialogVisible" title="历史方案详情" width="min(920px, 92vw)" destroy-on-close>
      <div v-loading="planDetailLoading" class="history-plan-detail">
        <header v-if="planDetail"><strong>{{ planDetail.planName }}</strong><span>{{ formatDateTime(planDetail.createdAt) }}</span></header>
        <div v-for="group in detailGroups" :key="group.schoolKey" class="history-plan-detail__row">
          <strong>{{ group.universityName }}</strong>
          <span>{{ group.majors.join("、") || "院校志愿" }}</span>
          <em>{{ group.obeyAdjustment ? "服从调剂" : "不服从调剂" }}</em>
        </div>
      </div>
    </el-dialog>
  </el-main>
</template>

<style scoped>
.plans-workspace { display: block; }
.plan-overview { padding: 24px 28px; border: 1px solid #ffd7bd; border-radius: 18px; background: linear-gradient(135deg, #fff9f4, #fff4e9); }
.plan-overview__profile,.plan-overview__progress,.plan-overview__toolbar { display: flex; align-items: center; gap: 14px; }
.plan-overview__profile strong { color: #172033; font-size: 20px; }.plan-overview__profile span { color: #744f38; }.plan-overview__profile b { color: #ff650f; font-size: 22px; }.plan-overview__profile em { padding: 5px 12px; border-radius: 16px; background: #ff650f; color: #fff; font-style: normal; font-weight: 700; }
.plan-overview__progress { margin-top: 18px; }.plan-overview__progress span,.plan-overview__progress strong { color: #744f38; font-size: 13px; }.plan-overview__progress i { flex: 1; height: 10px; overflow: hidden; border-radius: 8px; background: #f7d8c3; }.plan-overview__progress i b { display: block; height: 100%; border-radius: inherit; background: #ff751d; }
.plan-overview__toolbar { margin-top: 18px; flex-wrap: wrap; }.plan-mode-switch { display: flex; padding: 4px; border-radius: 10px; background: #f1f3f6; }.plan-mode-switch button { padding: 10px 18px; border: 0; border-radius: 8px; background: transparent; color: #5f6878; cursor: pointer; }.plan-mode-switch button.is-active { background: #fff; color: #ff650f; font-weight: 700; box-shadow: 0 2px 8px rgba(30,41,59,.08); }
.plan-strategy-summary { display: flex; align-items: center; gap: 12px; padding: 20px 0; }.plan-strategy-summary span { padding: 8px 17px; border-radius: 22px; font-weight: 700; }.plan-strategy-summary small { margin-left: auto; color: #929bad; }.is-rush { background: #fff0e5; color: #ff650f; }.is-safe { background: #e8f2ff; color: #2f83ff; }.is-guarantee { background: #e9f8ef; color: #0aa65b; }
.plan-detail-mode { display: grid; gap: 18px; }.plan-strategy-block { overflow: hidden; border: 1px solid #e8ebf0; border-radius: 16px; background: #fff; }.plan-strategy-block > header { display: flex; align-items: center; gap: 12px; padding: 16px 22px; border-bottom: 1px solid #eef0f3; }.plan-strategy-block h2 { margin: 0; padding-left: 10px; border-left: 5px solid #ff650f; font-size: 18px; }.plan-strategy-block header span { color: #929bad; }
.plan-school-row { display: grid; grid-template-columns: 38px minmax(180px, .9fr) minmax(260px, 1.5fr) 112px 106px 40px; align-items: center; gap: 14px; min-height: 82px; padding: 12px 20px; border-bottom: 1px solid #eef0f3; }.plan-school-row:last-child { border-bottom: 0; }.plan-school-row > i { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 9px; background: #fff1e6; color: #ff650f; font-style: normal; font-weight: 700; }.plan-school-row__school { display: flex; align-items: center; gap: 10px; min-width: 0; }.plan-school-row__school strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.plan-school-row__majors { display: flex; flex-wrap: wrap; gap: 7px; }.plan-school-row__majors span { padding: 6px 9px; border-radius: 6px; background: #f3f4f6; color: #616b7b; font-size: 12px; }.plan-school-row__majors em { color: #a0a7b2; font-style: normal; }
.adjustment-toggle { padding: 8px 12px; border: 1px solid #c6ccd5; border-radius: 18px; background: #fff; color: #7e8795; cursor: pointer; }.adjustment-toggle.is-on { border-color: #0cad62; background: #effcf5; color: #079c56; font-weight: 700; }.plan-probability { display: inline-flex; align-items: center; justify-content: center; justify-self: start; min-height: 32px; padding: 5px 11px; border-radius: 8px; font-weight: 700; white-space: nowrap; }.plan-probability.is-rush { color: #ff650f; }.plan-probability.is-safe { color: #2f83ff; }.plan-probability.is-guarantee { color: #0aa65b; }
.plan-table-wrap { overflow-x: auto; border: 1px solid #e7eaf0; border-radius: 16px; background: #fff; }.plan-table { width: 100%; min-width: 820px; border-collapse: collapse; }.plan-table th,.plan-table td { padding: 16px 18px; border-bottom: 1px solid #eceff3; text-align: left; }.plan-table th { background: #f6f7f9; color: #38445a; }.plan-table td:nth-child(2) { display: flex; align-items: center; gap: 10px; }.adjustment-text { border: 0; background: transparent; color: #079c56; cursor: pointer; }
.history-plan-detail > header { display: flex; justify-content: space-between; margin-bottom: 14px; }.history-plan-detail > header span { color: #9098a6; }.history-plan-detail__row { display: grid; grid-template-columns: 180px 1fr 100px; gap: 16px; padding: 14px; border-top: 1px solid #eceff3; }.history-plan-detail__row em { color: #079c56; font-style: normal; }
@media (max-width: 900px) { .plan-school-row { grid-template-columns: 36px 1fr 110px; }.plan-school-row__majors { grid-column: 2 / -1; }.plan-probability { grid-column: 2; }.plan-strategy-summary small { display: none; } }
@media (max-width: 600px) { .plan-overview { padding: 20px; }.plan-overview__profile { align-items: flex-start; flex-wrap: wrap; }.plan-overview__progress { align-items: stretch; flex-direction: column; }.plan-overview__toolbar .el-button { flex: 1; }.plan-mode-switch { width: 100%; }.plan-mode-switch button { flex: 1; }.plan-school-row { grid-template-columns: 32px 1fr 36px; padding: 14px; }.plan-school-row__school { grid-column: 2; }.plan-school-row__majors { grid-column: 2 / -1; }.adjustment-toggle { grid-column: 2; justify-self: start; }.plan-probability { grid-column: 2; }.history-plan-detail__row { grid-template-columns: 1fr; } }
</style>
