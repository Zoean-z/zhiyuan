<script setup>
import { inject } from "vue";
import AgentWorkspace from "../components/AgentWorkspace.vue";

const { auth, navigateTo } = inject("workspace");
</script>

<template>
  <el-main class="app-main app-main--agent">
    <AgentWorkspace :token="auth?.token" :user="auth?.user" @jump-to-plans="navigateTo('plans')" />
  </el-main>
</template>
