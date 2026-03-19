<script setup>
import { formatDateTime, queryTypeLabel, queryTypeTag } from "../utils/recommendation";

defineProps({
  records: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

defineEmits(["refresh", "view"]);
</script>

<template>
  <el-card class="history-card" shadow="never">
    <template #header>
      <div class="panel-title-row">
        <span>历史记录</span>
        <el-button type="primary" plain size="small" @click="$emit('refresh')">刷新</el-button>
      </div>
    </template>

    <el-table v-if="records.length" :data="records" v-loading="loading" border>
      <el-table-column label="查询时间" min-width="180">
        <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="查询类型" width="120">
        <template #default="scope">
          <el-tag :type="queryTypeTag(scope.row.queryType)" effect="light">
            {{ queryTypeLabel(scope.row.queryType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="queryContent" label="查询内容" min-width="320" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="$emit('view', scope.row)">查看结果</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else :image-size="100" description="暂无历史记录" />
  </el-card>
</template>
