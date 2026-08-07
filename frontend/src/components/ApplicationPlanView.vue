<script setup>
import { computed, ref } from "vue";
import { Document, Search } from "@element-plus/icons-vue";
import { buildGroupedFromResult, formatDateTime, sourceTypeLabel, sourceTypeTag } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";

const props = defineProps({
  records: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
});

const emit = defineEmits(["refresh", "view", "delete"]);

const searchQuery = ref("");
const sourceFilter = ref("all");
const sortBy = ref("createdAtDesc");

const SOURCE_OPTIONS = [
  { value: "all", label: "全部" },
  { value: "score", label: "分数查询" },
  { value: "text", label: "文本查询" }
];

const SORT_OPTIONS = [
  { value: "createdAtDesc", label: "创建时间（最新）" },
  { value: "createdAtAsc", label: "创建时间（最早）" },
  { value: "nameAsc", label: "方案名称（A-Z）" }
];

const filteredRecords = computed(() => {
  let result = [...props.records];

  if (sourceFilter.value !== "all") {
    result = result.filter((r) => r.sourceType === sourceFilter.value);
  }

  const keyword = searchQuery.value.trim().toLowerCase();
  if (keyword) {
    result = result.filter((r) => {
      const name = (r.planName || "").toLowerCase();
      const query = (r.sourceQuery || "").toLowerCase();
      return name.includes(keyword) || query.includes(keyword);
    });
  }

  if (sortBy.value === "createdAtDesc") {
    result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  } else if (sortBy.value === "createdAtAsc") {
    result.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  } else if (sortBy.value === "nameAsc") {
    result.sort((a, b) => (a.planName || "").localeCompare(b.planName || ""));
  }

  return result;
});

const stats = computed(() => {
  const all = props.records;
  const total = all.length;
  const draftCount = all.filter((r) => r.planName === "当前方案草稿").length;
  const sorted = [...all].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  const latest = sorted[0];
  return { total, draftCount, latest };
});

function getPlanCounts(record) {
  let parsed = null;
  try {
    parsed = record.resultJson ? JSON.parse(record.resultJson) : null;
  } catch {
    parsed = null;
  }
  if (!parsed) return { rush: 0, safe: 0, guarantee: 0 };
  const grouped = buildGroupedFromResult(parsed);
  return {
    rush: grouped.rush.length,
    safe: grouped.safe.length,
    guarantee: grouped.guarantee.length
  };
}

function getSourceQueryInfo(sourceQuery) {
  if (!sourceQuery) return {};
  const info = {};
  const modeMatch = sourceQuery.match(/模式[：:]\s*([^\s，,]+)/);
  const scoreMatch = sourceQuery.match(/分数[：:]\s*(\d+)/);
  const provinceMatch = sourceQuery.match(/省份[：:]\s*([^\s，,]+)/);
  const subjectMatch = sourceQuery.match(/科类[：:]\s*([^\s，,]+)/);
  const majorMatch = sourceQuery.match(/专业[：:]\s*(.+?)$/);
  if (modeMatch) info.mode = modeMatch[1];
  if (scoreMatch) info.score = scoreMatch[1];
  if (provinceMatch) info.province = provinceMatch[1];
  if (subjectMatch) info.subject = subjectMatch[1];
  if (majorMatch) info.major = majorMatch[1];
  return info;
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

function isToday(dateStr) {
  if (!dateStr) return false;
  const d = new Date(dateStr);
  const now = new Date();
  return d.getFullYear() === now.getFullYear()
    && d.getMonth() === now.getMonth()
    && d.getDate() === now.getDate();
}

function formatRelativeDate(dateStr) {
  if (!dateStr) return "-";
  if (isToday(dateStr)) return "今天";
  const d = new Date(dateStr);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}/${m}/${day}`;
}
</script>

<template>
  <div class="plans-page">
    <div class="plans-stats">
      <div class="stats-card stats-card--blue">
        <div class="stats-card__icon stats-card__icon--blue">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stats-card__info">
          <span class="stats-card__label">方案总数</span>
          <span class="stats-card__value">{{ stats.total }}</span>
          <span class="stats-card__desc">我的全部方案</span>
        </div>
      </div>
      <div class="stats-card stats-card--green">
        <div class="stats-card__icon stats-card__icon--green">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stats-card__info">
          <span class="stats-card__label">当前草稿</span>
          <span class="stats-card__value">{{ stats.draftCount }}</span>
          <span class="stats-card__desc">待完善方案</span>
        </div>
      </div>
      <div class="stats-card stats-card--purple">
        <div class="stats-card__icon stats-card__icon--purple">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stats-card__info">
          <span class="stats-card__label">最近更新</span>
          <span class="stats-card__value">{{ stats.latest ? formatRelativeDate(stats.latest.createdAt) : "-" }}</span>
          <span class="stats-card__desc">{{ stats.latest ? formatDate(stats.latest.createdAt) + " 更新" : "暂无更新" }}</span>
        </div>
      </div>
    </div>

    <div class="plans-toolbar">
      <div class="plans-toolbar__left">
        <el-input
          v-model="searchQuery"
          placeholder="搜索方案名称 / 专业 / 学校"
          :prefix-icon="Search"
          clearable
          class="plans-search"
        />
        <div class="plans-toolbar__filters">
          <span class="plans-toolbar__filter-label">来源类型</span>
          <div class="plans-filter-group">
            <button
              v-for="opt in SOURCE_OPTIONS"
              :key="opt.value"
              class="plans-filter-btn"
              :class="{ 'is-active': sourceFilter === opt.value }"
              @click="sourceFilter = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
        <div class="plans-toolbar__sort">
          <span class="plans-toolbar__filter-label">排序方式</span>
          <el-select v-model="sortBy" size="default" style="width: 160px;">
            <el-option v-for="opt in SORT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="plans-list">
      <div v-if="!loading && !filteredRecords.length" class="plans-empty">
        <el-empty :image-size="100" :description="UI_TEXT.empty.plans" />
      </div>
      <div
        v-for="record in filteredRecords"
        :key="record.id"
        class="plan-card"
      >
        <div class="plan-card__left">
          <div class="plan-card__icon">
            <el-icon :size="28" color="#409eff"><Document /></el-icon>
          </div>
          <div class="plan-card__info">
            <h3 class="plan-card__name">{{ record.planName }}</h3>
            <span class="plan-card__time">创建时间</span>
            <span class="plan-card__time">{{ formatDate(record.createdAt) }}</span>
          </div>
        </div>

        <div class="plan-card__middle">
          <span class="plan-source-tag" :class="`plan-source-tag--${record.sourceType}`">
            {{ sourceTypeLabel(record.sourceType) }}
          </span>
          <div v-if="record.sourceQuery" class="plan-card__query">
            <span v-if="getSourceQueryInfo(record.sourceQuery).mode">模式：{{ getSourceQueryInfo(record.sourceQuery).mode }}</span>
            <span v-if="getSourceQueryInfo(record.sourceQuery).score">，分数：{{ getSourceQueryInfo(record.sourceQuery).score }}</span>
            <span v-if="getSourceQueryInfo(record.sourceQuery).province">，省份：{{ getSourceQueryInfo(record.sourceQuery).province }}</span>
            <br />
            <span v-if="getSourceQueryInfo(record.sourceQuery).subject">科类：{{ getSourceQueryInfo(record.sourceQuery).subject }}</span>
            <span v-if="getSourceQueryInfo(record.sourceQuery).major">，专业：{{ getSourceQueryInfo(record.sourceQuery).major }}</span>
          </div>
          <div v-else class="plan-card__query plan-card__query--muted">
            {{ record.sourceQuery || "无来源信息" }}
          </div>
        </div>

        <div class="plan-card__counts">
          <div class="plan-card__count-item">
            <span class="plan-card__count-label">冲刺</span>
            <span class="plan-card__count-value plan-card__count-value--rush">{{ getPlanCounts(record).rush }}</span>
          </div>
          <div class="plan-card__count-item">
            <span class="plan-card__count-label">稳妥</span>
            <span class="plan-card__count-value plan-card__count-value--safe">{{ getPlanCounts(record).safe }}</span>
          </div>
          <div class="plan-card__count-item">
            <span class="plan-card__count-label">保底</span>
            <span class="plan-card__count-value plan-card__count-value--guarantee">{{ getPlanCounts(record).guarantee }}</span>
          </div>
        </div>

        <div class="plan-card__actions">
          <button class="plan-action-btn plan-action-btn--view" @click="emit('view', record)">
            查看详情
          </button>
          <button class="plan-action-btn plan-action-btn--delete" @click="emit('delete', record)">
            删除
          </button>
        </div>
      </div>
    </div>

    <div class="plans-footer">
      <span class="plans-footer__count">共 {{ filteredRecords.length }} 个方案</span>
    </div>
  </div>
</template>

<style scoped>
.plans-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.plans-stats {
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

.plans-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border: 1px solid var(--border, #e6ebf2);
  border-radius: 14px;
  background: #fff;
}

.plans-toolbar__left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  flex-wrap: wrap;
}

.plans-search {
  width: 240px;
}

.plans-toolbar__filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plans-toolbar__filter-label {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.plans-filter-group {
  display: flex;
  gap: 4px;
}

.plans-filter-btn {
  padding: 6px 14px;
  border: 1px solid #e6ebf2;
  border-radius: 20px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.plans-filter-btn:hover {
  border-color: #1f6feb;
  color: #1f6feb;
}

.plans-filter-btn.is-active {
  background: #1f6feb;
  border-color: #1f6feb;
  color: #fff;
}

.plans-toolbar__sort {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plans-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.plans-empty {
  padding: 60px 0;
}

.plan-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  border: 1px solid var(--border, #e6ebf2);
  border-radius: 14px;
  background: #fff;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.plan-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border-color: #c8d6e5;
}

.plan-card__left {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 220px;
}

.plan-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e8f0fe 0%, #d6e4ff 100%);
  flex-shrink: 0;
}

.plan-card__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.plan-card__name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 22px;
}

.plan-card__draft-tag {
  margin: 0;
}

.plan-card__name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.plan-card__time {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.plan-card__middle {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.plan-source-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
  width: fit-content;
}

.plan-source-tag--score {
  background: #dcfce7;
  color: #16a34a;
}

.plan-source-tag--text {
  background: #fef3c7;
  color: #d97706;
}

.plan-card__query {
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.plan-card__query--muted {
  color: #94a3b8;
}

.plan-card__counts {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
}

.plan-card__count-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.plan-card__count-label {
  color: #64748b;
  font-size: 12px;
}

.plan-card__count-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.plan-card__count-value--rush {
  color: #ef4444;
}

.plan-card__count-value--safe {
  color: #f59e0b;
}

.plan-card__count-value--guarantee {
  color: #16a34a;
}

.plan-card__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.plan-action-btn {
  padding: 6px 16px;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid;
}

.plan-action-btn--view {
  background: #f0f7ff;
  border-color: #d0e3ff;
  color: #1f6feb;
}

.plan-action-btn--view:hover {
  background: #e0f0ff;
  border-color: #b0d0ff;
}

.plan-action-btn--delete {
  background: #fff0f0;
  border-color: #ffd0d0;
  color: #ef4444;
}

.plan-action-btn--delete:hover {
  background: #ffe0e0;
  border-color: #ffb0b0;
}

.plans-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.plans-footer__count {
  color: #64748b;
  font-size: 13px;
}
</style>
