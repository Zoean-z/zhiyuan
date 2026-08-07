<script setup>
import { inject, onMounted, onUnmounted, ref } from "vue";
import ApplicationPlanView from "../components/ApplicationPlanView.vue";
import RecommendationResult from "../components/RecommendationResult.vue";
import { formatDateTime, sourceTypeLabel } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";

const {
  deletePlan, invalidatePlanLoad, loadPlans, openPlanDetail, planAiSummary, planDetail,
  planDetailLoading, planDialogVisible, planFinalAdvice, planGrouped,
  planHasResult, planLoading, planRecommendationMode, planRecords,
  planResultJson, planSummary, planTips, resetPlanDialog, updatePlanDetailItems
} = inject("workspace");

const editing = ref(false);
const editSubmitting = ref(false);
const editableItems = ref([]);

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
    }
  } finally {
    editSubmitting.value = false;
  }
}

onMounted(loadPlans);
onUnmounted(() => {
  invalidatePlanLoad();
  planDialogVisible.value = false;
  resetPlanDialog();
});
</script>

<template>
  <el-main class="app-main">
    <ApplicationPlanView :records="planRecords" :loading="planLoading" @refresh="loadPlans" @view="handleOpenPlan" @delete="deletePlan" />

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
              <el-button v-if="!editing" type="primary" plain @click="startEditing">编辑志愿表</el-button>
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
                <el-option label="冲刺" value="rush" />
                <el-option label="稳妥" value="safe" />
                <el-option label="保底" value="guarantee" />
              </el-select>
              <el-button type="danger" link @click="removeEditableItem(index)">删除</el-button>
            </div>
            <el-empty v-if="!editableItems.length" description="该志愿表暂无志愿" :image-size="80" />
          </div>

          <RecommendationResult v-else-if="planHasResult" :loading="false" :grouped="planGrouped" :summary="planSummary" :ai-summary="planAiSummary" :final-advice="planFinalAdvice" :tips="planTips" :recommendation-mode="planRecommendationMode" />
          <el-card v-else shadow="never" class="history-raw-card">
            <template #header>原始结果</template>
            <pre class="history-raw">{{ planResultJson || UI_TEXT.common.noDisplayContent }}</pre>
          </el-card>
        </template>
      </el-skeleton>
    </el-dialog>
  </el-main>
</template>
