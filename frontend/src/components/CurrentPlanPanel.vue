<script setup>
import { computed } from "vue";
import { strategyLabel, strategyTagType } from "../utils/recommendation";

const props = defineProps({
  items: { type: Array, default: () => [] },
  saveDisabled: { type: Boolean, default: false },
  clearingDisabled: { type: Boolean, default: false }
});

const emit = defineEmits(["remove", "clear", "save"]);
const itemCount = computed(() => props.items.length);
</script>

<template>
  <el-card class="current-plan-card" shadow="never">
    <template #header>
      <div class="panel-title-row current-plan-card__header">
        <div>
          <span>当前方案</span>
          <div class="current-plan-card__count">已选 {{ itemCount }} 条</div>
        </div>
        <div class="current-plan-card__actions">
          <el-button plain size="small" :disabled="clearingDisabled" @click="emit('clear')">清空</el-button>
          <el-button type="primary" size="small" :disabled="saveDisabled" @click="emit('save')">保存方案</el-button>
        </div>
      </div>
    </template>

    <div v-if="items.length" class="current-plan-list">
      <div v-for="item in items" :key="item.planKey" class="current-plan-item">
        <div class="current-plan-item__main">
          <div class="current-plan-item__title">{{ item.universityName }}</div>
          <div v-if="item.majorName" class="current-plan-item__subtitle">{{ item.majorName }}</div>
        </div>
        <div class="current-plan-item__side">
          <el-tag size="small" :type="strategyTagType(item.strategy)" effect="light">{{ strategyLabel(item.strategy) }}</el-tag>
          <el-button type="danger" link @click="emit('remove', item)">删除</el-button>
        </div>
      </div>
    </div>

    <el-empty v-else description="当前方案为空，请从右侧推荐结果中加入条目" :image-size="90" />
  </el-card>
</template>
