<script setup>
import { formatDateTime, sourceTypeLabel, sourceTypeTag } from "../utils/recommendation";
import { UI_TEXT } from "../utils/ui";
defineProps({ records: { type: Array, default: () => [] }, loading: { type: Boolean, default: false } });
defineEmits(["refresh", "view", "delete"]);
</script>

<template>
  <el-card class="history-card" shadow="never">
    <template #header>
      <div class="panel-title-row">
        <span>志愿方案</span>
        <el-button type="primary" plain size="small" @click="$emit('refresh')">刷新</el-button>
      </div>
    </template>

    <el-table v-if="records.length" :data="records" v-loading="loading" border>
      <el-table-column prop="planName" label="方案名称" min-width="180" />
      <el-table-column label="创建时间" min-width="180">
        <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="来源类型" width="140">
        <template #default="scope">
          <el-tag :type="sourceTypeTag(scope.row.sourceType)" effect="light">{{ sourceTypeLabel(scope.row.sourceType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceQuery" label="来源内容" min-width="320" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="$emit('view', scope.row)">查看详情</el-button>
          <el-button type="danger" link @click="$emit('delete', scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else :image-size="100" :description="UI_TEXT.empty.plans" />
  </el-card>
</template>
