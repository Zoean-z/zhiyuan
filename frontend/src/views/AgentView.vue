<script setup>
import { inject } from "vue";
import AgentWorkspace from "../components/AgentWorkspace.vue";

const { auth, navigateTo } = inject("workspace");
</script>

<template>
  <div class="xz-page">
    <AgentWorkspace :token="auth?.token" :user="auth?.user" @jump-to-plans="navigateTo('plans')" />
  </div>
</template>
