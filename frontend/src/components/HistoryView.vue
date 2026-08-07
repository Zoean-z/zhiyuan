<script setup>
import { computed, ref } from "vue";
import { Clock, Delete, Document, Search, View } from "@element-plus/icons-vue";
import { formatDateTime, queryTypeLabel, queryTypeTag } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";

const props = defineProps({
  records: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
});

const emit = defineEmits(["refresh", "view", "delete", "reQuery"]);

const searchQuery = ref("");
const typeFilter = ref("all");
const timeRange = ref("30");
const sortBy = ref("timeDesc");
const currentPage = ref(1);
const pageSize = ref(10);

const TYPE_OPTIONS = [
  { value: "all", label: "全部" },
  { value: "score", label: "分数查询" },
  { value: "text", label: "文本查询" },
  { value: "agent", label: "AI 对话" }
];

const TIME_OPTIONS = [
  { value: "7", label: "最近7天" },
  { value: "30", label: "最近30天" },
  { value: "90", label: "最近90天" },
  { value: "all", label: "全部时间" }
];

const SORT_OPTIONS = [
  { value: "timeDesc", label: "按时间倒序" },
  { value: "timeAsc", label: "按时间正序" }
];

const filteredRecords = computed(() => {
  let result = [...props.records];

  if (typeFilter.value !== "all") {
    result = result.filter((r) => r.queryType === typeFilter.value);
  }

  const keyword = searchQuery.value.trim().toLowerCase();
  if (keyword) {
    result = result.filter((r) => {
      const content = (r.queryContent || "").toLowerCase();
      return content.includes(keyword);
    });
  }

  if (timeRange.value !== "all") {
    const days = parseInt(timeRange.value);
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - days);
    result = result.filter((r) => new Date(r.createdAt) >= cutoff);
  }

  if (sortBy.value === "timeDesc") {
    result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  } else {
    result.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  }

  return result;
});

const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRecords.value.slice(start, start + pageSize.value);
});

const stats = computed(() => {
  const all = props.records;
  const total = all.length;
  const now = new Date();
  const weekAgo = new Date(now - 7 * 24 * 60 * 60 * 1000);
  const lastWeek = all.filter((r) => new Date(r.createdAt) >= weekAgo).length;

  const typeCounts = { score: 0, text: 0, agent: 0 };
  all.forEach((r) => {
    if (typeCounts[r.queryType] !== undefined) {
      typeCounts[r.queryType]++;
    }
  });
  const mostUsedType = Object.entries(typeCounts).sort((a, b) => b[1] - a[1])[0];
  const mostUsedLabel = mostUsedType ? queryTypeLabel(mostUsedType[0]) : "-";
  const mostUsedPercent = total > 0 ? Math.round((mostUsedType[1] / total) * 100) : 0;

  return { total, lastWeek, mostUsedLabel, mostUsedPercent };
});

function getQueryTypeClass(type) {
  if (type === "score") return "history-type-tag--score";
  if (type === "text") return "history-type-tag--text";
  if (type === "agent") return "history-type-tag--agent";
  return "";
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const h = String(d.getHours()).padStart(2, "0");
  const min = String(d.getMinutes()).padStart(2, "0");
  const sec = String(d.getSeconds()).padStart(2, "0");
  return `${y}/${m}/${day} ${h}:${min}:${sec}`;
}

function handlePageChange(page) {
  currentPage.value = page;
}

function handleSizeChange(size) {
  pageSize.value = size;
  currentPage.value = 1;
}
</script>

<template>
  <div class="history-page">
    <div class="history-stats">
      <div class="stats-card stats-card--blue">
        <div class="stats-card__icon stats-card__icon--blue">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stats-card__info">
          <span class="stats-card__label">总查询次数</span>
          <span class="stats-card__value">{{ stats.total }}</span>
          <span class="stats-card__desc">全部历史查询记录</span>
        </div>
      </div>
      <div class="stats-card stats-card--green">
        <div class="stats-card__icon stats-card__icon--green">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="stats-card__info">
          <span class="stats-card__label">近7天查询</span>
          <span class="stats-card__value">{{ stats.lastWeek }}</span>
          <span class="stats-card__desc">较上周 <span class="stats-card__trend">↑ 12%</span></span>
        </div>
      </div>
      <div class="stats-card stats-card--purple">
        <div class="stats-card__icon stats-card__icon--purple">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stats-card__info">
          <span class="stats-card__label">常用查询类型</span>
          <span class="stats-card__value">{{ stats.mostUsedLabel }}</span>
          <span class="stats-card__desc">占比 {{ stats.mostUsedPercent }}%</span>
        </div>
      </div>
    </div>

    <div class="history-toolbar">
      <div class="history-toolbar__left">
        <el-input
          v-model="searchQuery"
          placeholder="搜索查询内容 / 学校 / 专业"
          :prefix-icon="Search"
          clearable
          class="history-search"
        />
        <div class="history-toolbar__filters">
          <span class="history-toolbar__filter-label">类型筛选</span>
          <div class="history-filter-group">
            <button
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              class="history-filter-btn"
              :class="{ 'is-active': typeFilter === opt.value }"
              @click="typeFilter = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
        <div class="history-toolbar__time">
          <span class="history-toolbar__filter-label">时间范围</span>
          <el-select v-model="timeRange" size="default" style="width: 140px;">
            <el-option v-for="opt in TIME_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <div class="history-toolbar__sort">
          <span class="history-toolbar__filter-label">排序方式</span>
          <el-select v-model="sortBy" size="default" style="width: 140px;">
            <el-option v-for="opt in SORT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
      </div>
    </div>

    <div class="history-list-header">
      <h3 class="history-list-title">查询记录列表</h3>
      <div class="history-list-actions">
        <el-button type="primary" plain size="small" @click="emit('refresh')">
          <el-icon><Clock /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="history-list">
      <div v-if="!loading && !paginatedRecords.length" class="history-empty">
        <el-empty :image-size="100" :description="UI_TEXT.empty.history" />
      </div>
      <div v-else class="history-table">
        <div class="history-table__header">
          <span class="history-table__col history-table__col--time">查询时间</span>
          <span class="history-table__col history-table__col--type">类型</span>
          <span class="history-table__col history-table__col--content">查询内容</span>
          <span class="history-table__col history-table__col--summary">结果摘要</span>
          <span class="history-table__col history-table__col--actions">操作</span>
        </div>
        <div
          v-for="record in paginatedRecords"
          :key="record.id"
          class="history-table__row"
        >
          <span class="history-table__cell history-table__cell--time">
            {{ formatDate(record.createdAt) }}
          </span>
          <span class="history-table__cell history-table__cell--type">
            <span class="history-type-tag" :class="getQueryTypeClass(record.queryType)">
              {{ queryTypeLabel(record.queryType) }}
            </span>
          </span>
          <span class="history-table__cell history-table__cell--content" :title="record.queryContent">
            {{ record.queryContent }}
          </span>
          <span class="history-table__cell history-table__cell--summary">
            {{ record.summary || "-" }}
          </span>
          <span class="history-table__cell history-table__cell--actions">
            <button class="history-action-btn history-action-btn--view" @click="emit('view', record)">
              查看结果
            </button>
            <button class="history-action-btn history-action-btn--query" @click="emit('reQuery', record)">
              再次查询
            </button>
            <button class="history-action-btn history-action-btn--delete" @click="emit('delete', record)">
              删除
            </button>
          </span>
        </div>
      </div>
    </div>

    <div class="history-footer">
      <span class="history-footer__count">共 {{ filteredRecords.length }} 条记录</span>
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredRecords.length"
        layout="sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.history-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.history-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stats-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border: 1px solid var(--border, #e6ebf2);
  border-radius: 14px;
  background: #fff;
  transition: box-shadow 0.2s;
}

.stats-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stats-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 14px;
}

.stats-card__icon--blue {
  background: linear-gradient(135deg, #e8f0fe 0%, #d6e4ff 100%);
  color: #1f6feb;
}

.stats-card__icon--green {
  background: linear-gradient(135deg, #e6f9ee 0%, #c8f5d8 100%);
  color: #16a34a;
}

.stats-card__icon--purple {
  background: linear-gradient(135deg, #f0e8fe 0%, #e0d4ff 100%);
  color: #7c3aed;
}

.stats-card__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stats-card__label {
  color: #64748b;
  font-size: 13px;
}

.stats-card__value {
  color: #1f2937;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stats-card__desc {
  color: #94a3b8;
  font-size: 12px;
}

.stats-card__trend {
  color: #16a34a;
  font-weight: 600;
}

.history-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border: 1px solid var(--border, #e6ebf2);
  border-radius: 14px;
  background: #fff;
}

.history-toolbar__left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  flex-wrap: wrap;
}

.history-search {
  width: 240px;
}

.history-toolbar__filters,
.history-toolbar__time,
.history-toolbar__sort {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-toolbar__filter-label {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.history-filter-group {
  display: flex;
  gap: 4px;
}

.history-filter-btn {
  padding: 6px 14px;
  border: 1px solid #e6ebf2;
  border-radius: 20px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-filter-btn:hover {
  border-color: #1f6feb;
  color: #1f6feb;
}

.history-filter-btn.is-active {
  background: #1f6feb;
  border-color: #1f6feb;
  color: #fff;
}

.history-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border: 1px solid var(--border, #e6ebf2);
  border-radius: 14px 14px 0 0;
  background: #fff;
}

.history-list-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.history-list {
  border: 1px solid var(--border, #e6ebf2);
  border-top: none;
  border-radius: 0 0 14px 14px;
  background: #fff;
  min-height: 200px;
}

.history-empty {
  padding: 60px 0;
}

.history-table {
  width: 100%;
}

.history-table__header {
  display: flex;
  padding: 12px 20px;
  background: #f8fafc;
  border-bottom: 1px solid var(--border, #e6ebf2);
}

.history-table__col {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.history-table__col--time {
  width: 180px;
}

.history-table__col--type {
  width: 120px;
}

.history-table__col--content {
  flex: 1;
}

.history-table__col--summary {
  width: 160px;
}

.history-table__col--actions {
  width: 240px;
  text-align: right;
}

.history-table__row {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border, #e6ebf2);
  transition: background 0.2s;
}

.history-table__row:last-child {
  border-bottom: none;
}

.history-table__row:hover {
  background: #f8fafc;
}

.history-table__cell {
  color: #334155;
  font-size: 14px;
  line-height: 1.5;
}

.history-table__cell--time {
  width: 180px;
  color: #64748b;
  font-size: 13px;
}

.history-table__cell--type {
  width: 120px;
}

.history-table__cell--content {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-table__cell--summary {
  width: 160px;
  color: #64748b;
  font-size: 13px;
}

.history-table__cell--actions {
  width: 240px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.history-type-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.history-type-tag--score {
  background: #dcfce7;
  color: #16a34a;
}

.history-type-tag--text {
  background: #fef3c7;
  color: #d97706;
}

.history-type-tag--agent {
  background: #e0f2fe;
  color: #0284c7;
}

.history-action-btn {
  padding: 5px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid;
}

.history-action-btn--view {
  background: #f0f7ff;
  border-color: #d0e3ff;
  color: #1f6feb;
}

.history-action-btn--view:hover {
  background: #e0f0ff;
  border-color: #b0d0ff;
}

.history-action-btn--query {
  background: #f0fdf4;
  border-color: #d0f5d8;
  color: #16a34a;
}

.history-action-btn--query:hover {
  background: #e0f9e8;
  border-color: #b0e8c0;
}

.history-action-btn--delete {
  background: #fff0f0;
  border-color: #ffd0d0;
  color: #ef4444;
}

.history-action-btn--delete:hover {
  background: #ffe0e0;
  border-color: #ffb0b0;
}

.history-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.history-footer__count {
  color: #64748b;
  font-size: 13px;
}
</style>
