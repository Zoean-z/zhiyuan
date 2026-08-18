<script setup>
import { Reading } from "@element-plus/icons-vue";

defineProps({
  admin: { type: Boolean, default: false }
});
</script>

<template>
  <div class="app-brand-lockup">
    <span class="app-brand-lockup__mark"><el-icon><Reading /></el-icon></span>
    <span class="app-brand-lockup__name">智愿AI报考平台</span>
    <span v-if="admin" class="app-brand-lockup__badge">管理界面</span>
  </div>
</template>
