<script setup>
import { inject, onMounted, onUnmounted } from "vue";
import HistoryView from "../components/HistoryView.vue";
import RecommendationResult from "../components/RecommendationResult.vue";
import { formatDateTime, queryTypeLabel } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";

const {
  deleteHistoryRecord, historyAiSummary, historyDetail, historyDetailLoading,
  historyDialogVisible, historyFinalAdvice, historyGrouped, historyHasResult,
  historyLoading, historyRecommendationMode, historyRecords, historyResultJson,
  historySummary, historyTips, invalidateHistoryLoad, loadHistory, openHistoryResult, resetHistoryDialog
} = inject("workspace");

onMounted(loadHistory);
onUnmounted(() => {
  invalidateHistoryLoad();
  historyDialogVisible.value = false;
  resetHistoryDialog();
});
</script>

<template>
  <el-main class="app-main">
    <HistoryView :records="historyRecords" :loading="historyLoading" @refresh="loadHistory" @view="openHistoryResult" @delete="deleteHistoryRecord" />

    <el-dialog v-model="historyDialogVisible" title="历史结果" width="80%" top="4vh" destroy-on-close>
      <el-skeleton :loading="historyDetailLoading" animated>
        <template #template>
          <el-skeleton-item variant="h1" style="width: 50%;" />
          <el-skeleton-item variant="text" style="margin-top: 8px;" />
          <el-skeleton-item variant="text" />
        </template>
        <template #default>
          <div v-if="historyDetail" class="history-detail-meta">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="查询时间">{{ formatDateTime(historyDetail.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="查询类型">{{ queryTypeLabel(historyDetail.queryType) }}</el-descriptions-item>
              <el-descriptions-item label="查询内容">{{ historyDetail.queryContent }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <RecommendationResult v-if="historyHasResult" :loading="false" :grouped="historyGrouped" :summary="historySummary" :ai-summary="historyAiSummary" :final-advice="historyFinalAdvice" :tips="historyTips" :recommendation-mode="historyRecommendationMode" />
          <el-card v-else shadow="never" class="history-raw-card">
            <template #header>原始结果</template>
            <pre class="history-raw">{{ historyResultJson || UI_TEXT.common.noDisplayContent }}</pre>
          </el-card>
        </template>
      </el-skeleton>
    </el-dialog>
  </el-main>
</template>
