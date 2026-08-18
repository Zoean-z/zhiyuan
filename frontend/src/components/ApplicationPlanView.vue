<script setup>
import { computed, ref } from "vue";
import { Delete, Search } from "@element-plus/icons-vue";
import { flattenPlanItems, formatDateTime, groupPlanItemsBySchool } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";

const props = defineProps({
  records: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
});

const emit = defineEmits(["refresh", "view", "load", "delete"]);
const searchQuery = ref("");

const historicalRecords = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase();
  return props.records
    .filter((record) => record.planName !== "当前方案草稿")
    .filter((record) => !keyword || String(record.planName || "").toLowerCase().includes(keyword))
    .slice()
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
});

function planMeta(record) {
  const items = flattenPlanItems(record.resultJson);
  return { schoolCount: groupPlanItemsBySchool(items).length, itemCount: items.length };
}
</script>

<template>
  <section class="saved-plan-section">
    <header class="saved-plan-section__head">
      <div>
        <p>已保存到后端的历史版本</p>
        <h2>我的填报方案（{{ historicalRecords.length }}）</h2>
      </div>
      <div class="saved-plan-section__actions">
        <el-input v-model="searchQuery" :prefix-icon="Search" clearable placeholder="搜索方案名称" />
        <el-button @click="emit('refresh')">刷新</el-button>
      </div>
    </header>

    <div v-loading="loading" class="saved-plan-grid">
      <article v-for="record in historicalRecords" :key="record.id" class="saved-plan-card">
        <button type="button" class="saved-plan-card__body" @click="emit('view', record)">
          <strong>{{ record.planName }}</strong>
          <span>{{ formatDateTime(record.createdAt) }}</span>
          <small>{{ planMeta(record).schoolCount }} 所院校 · {{ planMeta(record).itemCount }} 个专业志愿</small>
        </button>
        <div class="saved-plan-card__ops">
          <el-button plain @click="emit('load', record)">载入</el-button>
          <el-button type="danger" plain :icon="Delete" @click="emit('delete', record)">删除</el-button>
        </div>
      </article>
      <el-empty v-if="!loading && !historicalRecords.length" :description="UI_TEXT.empty.plans" :image-size="90" />
    </div>
  </section>
</template>

<style scoped>
.saved-plan-section { margin-top: 30px; padding: 26px; border: 1px solid #eceff3; border-radius: 18px; background: #fff; }
.saved-plan-section__head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 20px; }
.saved-plan-section__head p { margin: 0 0 5px; color: #9aa2af; font-size: 13px; }
.saved-plan-section__head h2 { margin: 0; color: #172033; font-size: 22px; }
.saved-plan-section__actions { display: flex; gap: 10px; width: min(440px, 100%); }
.saved-plan-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; min-height: 90px; }
.saved-plan-grid :deep(.el-empty) { grid-column: 1 / -1; }
.saved-plan-card { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 18px 20px; border: 1px solid #e8ebf0; border-radius: 14px; background: #fff; }
.saved-plan-card__body { min-width: 0; padding: 0; border: 0; background: transparent; color: inherit; text-align: left; cursor: pointer; }
.saved-plan-card__body strong,.saved-plan-card__body span,.saved-plan-card__body small { display: block; }
.saved-plan-card__body strong { overflow: hidden; color: #182034; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.saved-plan-card__body span { margin-top: 6px; color: #8d96a6; font-size: 12px; }
.saved-plan-card__body small { margin-top: 4px; color: #687386; }
.saved-plan-card__ops { display: flex; flex-shrink: 0; gap: 8px; }
@media (max-width: 860px) { .saved-plan-grid { grid-template-columns: 1fr; }.saved-plan-section__head { align-items: stretch; flex-direction: column; }.saved-plan-section__actions { width: 100%; } }
@media (max-width: 560px) { .saved-plan-section { padding: 18px; }.saved-plan-card { align-items: flex-start; flex-direction: column; }.saved-plan-card__ops { width: 100%; }.saved-plan-card__ops .el-button { flex: 1; } }
</style>
