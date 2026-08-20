<script setup>
import { computed, inject, onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import RecommendationResult from "../components/RecommendationResult.vue";
import { formatDateTime, sourceTypeLabel } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";
import { clearCurrentSheet, readCurrentSheet } from "../utils/volunteerCore";
import {
  describeSlot,
  itemsFromResultJson,
  loadPlanItemsIntoSheet,
  sheetGroups,
  sheetStats,
  sheetToPlanItems
} from "../utils/planSync";

/**
 * 我的志愿方案（重构）
 *
 * 【修复的问题】
 * 1. 「点这个志愿表，并不是一个志愿表管理的界面 / 而且点进来这里还有一个志愿表」：
 *    原页把「院校库选校（与 /volunteer 完全重复）」+「草稿编辑器」+「方案列表」塞在同一个
 *    四页签工作台里，于是出现“志愿表里又有一个志愿表”。
 *    现在本页只做一件事：管理方案（看 / 载入填报器 / 保存到云端 / 删除），
 *    所有“选校选专业”动作全部回到 /volunteer。
 * 2. 「这里用的是旧数据结构，旧的和新的只能留一个会冲突」：
 *    后端不改，所以两套结构用 utils/planSync.js 做唯一桥接：
 *    本地 45 个志愿位（编辑用）↔ 后端 rush/safe/guarantee items（存储用）。
 */

const router = useRouter();

const {
  clearCurrentPlan, currentPlanItems, deletePlan, invalidatePlanLoad,
  loadCurrentPlanDraft, loadPlans, openPlanDetail, planAiSummary, planDetail,
  planDetailLoading, planDialogVisible, planFinalAdvice, planGrouped,
  planHasResult, planLoading, planRecommendationMode, planRecords,
  planResultJson, planSummary, planTips, replaceCurrentPlanDraftItems, resetPlanDialog, updatePlanDetailItems
} = inject("workspace");

const editing = ref(false);
const editSubmitting = ref(false);
const editableItems = ref([]);
const syncing = ref(false);

/* ===== 本地填报器里的志愿表（新结构，45 个志愿位） ===== */
const localSheet = ref(readCurrentSheet());
const localStats = computed(() => sheetStats(localSheet.value || []));
const localGroups = computed(() => sheetGroups(localSheet.value || []).filter((g) => g.items.length));
const hasLocalSheet = computed(() => localStats.value.filled > 0);

function refreshLocal() {
  localSheet.value = readCurrentSheet();
}

/* ===== 云端方案（旧结构，后端 /api/plans） ===== */
const draftRecord = computed(() => (planRecords.value || []).find((r) => r.planName === "当前方案草稿") || null);
const savedPlans = computed(() => (planRecords.value || []).filter((r) => r.planName !== "当前方案草稿"));

function planCountsOf(record) {
  const items = itemsFromResultJson(record.resultJson);
  const counts = { rush: 0, safe: 0, guarantee: 0 };
  items.forEach((item) => {
    const key = ["rush", "safe", "guarantee"].includes(item.strategy) ? item.strategy : "safe";
    counts[key] += 1;
  });
  return counts;
}

/* ===== 动作 ===== */
function goFill() {
  router.push({ path: "/volunteer", query: { autostart: "1" } });
}

function goAgentPlan() {
  router.push({ path: "/agent", query: { q: "请基于我的分数位次和选科，一键生成专属冲稳保志愿方案" } });
}

async function clearLocal() {
  try {
    await ElMessageBox.confirm("确认清空本地志愿表？已保存到云端的方案不会受影响。", "清空志愿表", { type: "warning" });
  } catch {
    return;
  }
  clearCurrentSheet();
  refreshLocal();
  ElMessage.success("已清空本地志愿表");
}

/** 本地 45 个志愿位 → 云端草稿（后端 /api/plans/current） */
async function syncToCloud() {
  const items = sheetToPlanItems(localSheet.value || []);
  if (!items.length) {
    ElMessage.warning("本地志愿表是空的，先去模拟填报");
    return;
  }
  syncing.value = true;
  try {
    const savedCount = await replaceCurrentPlanDraftItems(items);
    await loadPlans();
    await loadCurrentPlanDraft();
    ElMessage.success(`已同步 ${savedCount} 条志愿到云端草稿`);
  } catch (ex) {
    console.error("[syncToCloud]", ex);
    ElMessage.error("同步失败，请确认已登录后重试");
  } finally {
    syncing.value = false;
  }
}

/** 云端方案 → 本地填报器（继续编辑） */
function loadIntoFiller(items, label) {
  const list = items || [];
  if (!list.length) {
    ElMessage.warning("该方案没有可载入的志愿");
    return;
  }
  loadPlanItemsIntoSheet(list);
  refreshLocal();
  ElMessage.success(`已把「${label}」载入填报器`);
  router.push({ path: "/volunteer", query: { autostart: "1" } });
}

function loadDraftIntoFiller() {
  loadIntoFiller(currentPlanItems.value || [], "云端草稿");
}

function loadRecordIntoFiller(record) {
  loadIntoFiller(itemsFromResultJson(record.resultJson), record.planName);
}

async function handleOpenPlan(row) {
  editing.value = false;
  await openPlanDetail(row);
}

function startEditing() {
  editableItems.value = [
    ...planGrouped.rush.map((item) => ({ ...item, strategy: "rush" })),
    ...planGrouped.safe.map((item) => ({ ...item, strategy: "safe" })),
    ...planGrouped.guarantee.map((item) => ({ ...item, strategy: "guarantee" }))
  ];
  editing.value = true;
}

function removeEditableItem(index) {
  editableItems.value.splice(index, 1);
}

async function saveEditing() {
  editSubmitting.value = true;
  try {
    if (await updatePlanDetailItems(editableItems.value)) {
      editing.value = false;
      await loadCurrentPlanDraft();
    }
  } finally {
    editSubmitting.value = false;
  }
}

onMounted(() => {
  refreshLocal();
  loadPlans();
  loadCurrentPlanDraft();
});
onUnmounted(() => {
  invalidatePlanLoad();
  planDialogVisible.value = false;
  resetPlanDialog();
});
</script>

<template>
  <div class="gk-page">
    <GkHeader active="我的志愿表" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="mnz-wb">
            <div class="mnz-wb__head">
              <div class="mnz-wb__heading">
                <h2 class="mnz-wb__title">我的志愿方案</h2>
                <p class="mnz-wb__desc">这里只管理方案：看、载入填报器、保存到云端、删除。选校选专业在「志愿填报」页完成</p>
              </div>
              <div class="mnz-form__actions mnz-wb__actions">
                <button type="button" class="mnz-wb__ai" @click="goAgentPlan"><i>AI</i>一键定制方案</button>
                <button type="button" class="mnz-sheet__op" @click="goFill">去模拟填报 &gt;</button>
              </div>
            </div>

            <!-- 卡片 1：本地填报器里的志愿表（新结构，45 个志愿位） -->
            <div class="mnz-sheet">
              <div class="mnz-sheet__head">
                <p class="mnz-sheet__title">
                  当前志愿表
                  <em>本地填报器 · 已填 {{ localStats.filled }}/{{ localStats.total }}</em>
                </p>
                <div class="mnz-sheet__ops">
                  <span class="mnz-plan__tags">
                    <i class="is-rush">冲 {{ localStats.rush }}</i>
                    <i class="is-safe">稳 {{ localStats.safe }}</i>
                    <i class="is-guarantee">保 {{ localStats.guard }}</i>
                  </span>
                  <button type="button" class="mnz-sheet__op" @click="goFill">继续填报</button>
                  <button
                    v-if="hasLocalSheet"
                    type="button"
                    class="mnz-sheet__op"
                    :disabled="syncing"
                    @click="syncToCloud"
                  >
                    {{ syncing ? "同步中…" : "保存到云端" }}
                  </button>
                  <button v-if="hasLocalSheet" type="button" class="mnz-sheet__op is-danger" @click="clearLocal">清空</button>
                </div>
              </div>

              <template v-if="hasLocalSheet">
                <div v-for="group in localGroups" :key="group.key" class="mnz-sheet__group">
                  <p class="mnz-sheet__gtitle" :class="`is-${group.key}`">{{ group.label }}<em>{{ group.items.length }} 条</em></p>
                  <div v-for="slot in group.items" :key="slot.position" class="mnz-sheet__row">
                    <i class="mnz-sheet__idx" :class="`is-${group.key}`">{{ slot.position }}</i>
                    <div class="mnz-sheet__info">
                      <p class="mnz-sheet__name">{{ slot.schoolName }}</p>
                      <p class="mnz-sheet__major">{{ describeSlot(slot) || "院校志愿" }}</p>
                    </div>
                    <span class="mnz-sheet__prob">录取概率 {{ slot.prob == null ? "-" : `${slot.prob}%` }}</span>
                    <span class="mnz-sheet__rank">{{ slot.adjust === false ? "不服从调剂" : "服从调剂" }}</span>
                  </div>
                </div>
              </template>

              <div v-else class="mnz-empty">
                <p class="mnz-empty__title">本地志愿表还是空的</p>
                <p class="mnz-empty__desc">先去「志愿填报」填完考生信息，再选院校专业；或直接把云端方案载入填报器</p>
                <div class="mnz-empty__ops">
                  <button type="button" class="mnz-empty__btn" @click="goFill">去模拟填报</button>
                  <button type="button" class="mnz-empty__btn is-ghost" @click="goAgentPlan">AI 定制方案</button>
                </div>
              </div>
            </div>

            <!-- 卡片 2：云端草稿（后端 /api/plans/current） -->
            <div v-if="(currentPlanItems || []).length" class="mnz-sheet">
              <div class="mnz-sheet__head">
                <p class="mnz-sheet__title">云端草稿<em>后端已存 {{ (currentPlanItems || []).length }} 条</em></p>
                <div class="mnz-sheet__ops">
                  <button type="button" class="mnz-sheet__op" @click="loadDraftIntoFiller">载入填报器继续编辑</button>
                  <button v-if="draftRecord" type="button" class="mnz-sheet__op" @click="handleOpenPlan(draftRecord)">查看详情</button>
                  <button type="button" class="mnz-sheet__op is-danger" @click="clearCurrentPlan">清空云端草稿</button>
                </div>
              </div>
              <p class="gkd-note">
                云端存的是后端原有的扇形结构（rush / safe / guarantee），与本地 45 个志愿位通过
                <code>utils/planSync.js</code> 互转：同步时志愿位序号 → 涨度，载入时涨度 → 对应段的空位，不会互相覆盖。
              </p>
            </div>

            <!-- 卡片 3：已保存方案 -->
            <div class="mnz-plans">
              <div class="mnz-plans__head">
                <p class="mnz-sheet__title">已保存方案<em>共 {{ savedPlans.length }} 份</em></p>
              </div>
              <div v-if="planLoading" class="mnz-plans__blank">加载中…</div>
              <div v-else-if="savedPlans.length" class="mnz-plans__grid">
                <div v-for="record in savedPlans" :key="record.id" class="mnz-plan">
                  <div class="mnz-plan__top">
                    <p class="mnz-plan__name">{{ record.planName }}</p>
                    <span class="mnz-plan__source">{{ sourceTypeLabel(record.sourceType) }}</span>
                  </div>
                  <p class="mnz-plan__meta">{{ formatDateTime(record.createdAt) }}</p>
                  <div class="mnz-plan__tags">
                    <i class="is-rush">冲 {{ planCountsOf(record).rush }}</i>
                    <i class="is-safe">稳 {{ planCountsOf(record).safe }}</i>
                    <i class="is-guarantee">保 {{ planCountsOf(record).guarantee }}</i>
                  </div>
                  <div class="mnz-plan__ops">
                    <button type="button" @click="handleOpenPlan(record)">查看详情</button>
                    <button type="button" @click="loadRecordIntoFiller(record)">载入填报器</button>
                    <button type="button" class="is-danger" @click="deletePlan(record)">删除</button>
                  </div>
                </div>
              </div>
              <div v-else class="mnz-plans__blank">暂无保存的方案：在本地志愿表里点「保存到云端」，或让 AI 定制后保存</div>
            </div>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>

    <el-dialog v-model="planDialogVisible" title="方案详情" width="80%" top="4vh" destroy-on-close>
      <el-skeleton :loading="planDetailLoading" animated>
        <template #template>
          <el-skeleton-item variant="h1" style="width: 50%;" />
          <el-skeleton-item variant="text" style="margin-top: 8px;" />
          <el-skeleton-item variant="text" />
        </template>
        <template #default>
          <div v-if="planDetail" class="history-detail-meta">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="方案名称">{{ planDetail.planName }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">{{ sourceTypeLabel(planDetail.sourceType) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(planDetail.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="来源内容">{{ planDetail.sourceQuery }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div v-if="planHasResult" class="plan-detail-toolbar">
            <div>
              <strong>{{ planDetail.planName }}</strong>
              <span>共 {{ planGrouped.rush.length + planGrouped.safe.length + planGrouped.guarantee.length }} 条志愿</span>
            </div>
            <div>
              <el-button v-if="!editing" type="primary" plain @click="startEditing">调整涨度</el-button>
              <template v-else>
                <el-button @click="editing = false">取消</el-button>
                <el-button type="primary" :loading="editSubmitting" @click="saveEditing">保存修改</el-button>
              </template>
            </div>
          </div>

          <div v-if="editing" class="plan-editor-list">
            <div v-for="(item, index) in editableItems" :key="`${item.universityName}-${item.majorName}-${index}`" class="plan-editor-item">
              <div class="plan-editor-item__identity">
                <strong>{{ item.universityName }}</strong>
                <span>{{ item.majorName || "院校志愿" }}</span>
              </div>
              <div class="plan-editor-item__metrics">
                <span>录取概率 {{ item.admissionProbability == null ? "-" : `${item.admissionProbability}%` }}</span>
                <span>位次 {{ item.minRank ?? "-" }}</span>
              </div>
              <el-select v-model="item.strategy" class="plan-editor-item__strategy" aria-label="志愿档位">
                <el-option label="冲刷" value="rush" />
                <el-option label="稳妥" value="safe" />
                <el-option label="保底" value="guarantee" />
              </el-select>
              <el-button type="danger" link @click="removeEditableItem(index)">删除</el-button>
            </div>
            <el-empty v-if="!editableItems.length" description="该方案暂无志愿" :image-size="80" />
          </div>

          <RecommendationResult v-else-if="planHasResult" :loading="false" :grouped="planGrouped" :summary="planSummary" :ai-summary="planAiSummary" :final-advice="planFinalAdvice" :tips="planTips" :recommendation-mode="planRecommendationMode" />
          <el-card v-else shadow="never" class="history-raw-card">
            <template #header>原始结果</template>
            <pre class="history-raw">{{ planResultJson || UI_TEXT.common.noDisplayContent }}</pre>
          </el-card>
        </template>
      </el-skeleton>
    </el-dialog>
  </div>
</template>

<style scoped>
.plan-detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0;
  padding: 12px 16px;
  border: 1px solid var(--border, #e6ebf2);
  border-radius: 10px;
  background: #f8fbff;
}

.plan-detail-toolbar strong {
  margin-right: 12px;
  font-size: 16px;
}

.plan-detail-toolbar span {
  color: #64748b;
  font-size: 13px;
}

.plan-editor-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.plan-editor-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--border, #e6ebf2);
  border-radius: 12px;
  background: linear-gradient(180deg, #fbfdff 0%, #ffffff 100%);
}

.plan-editor-item__identity {
  flex: 1;
  min-width: 0;
}

.plan-editor-item__identity strong {
  display: block;
  font-size: 14px;
  line-height: 1.4;
}

.plan-editor-item__identity span {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 13px;
}

.plan-editor-item__metrics {
  display: flex;
  gap: 16px;
  color: #64748b;
  font-size: 13px;
  flex-shrink: 0;
}

.plan-editor-item__strategy {
  width: 120px;
  flex-shrink: 0;
}

.history-raw {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  color: #334155;
  line-height: 1.6;
}
</style>
