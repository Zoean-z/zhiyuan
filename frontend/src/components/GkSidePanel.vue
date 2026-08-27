<script setup>
import { useRouter } from "vue-router";

const router = useRouter();
</script>

<template>
  <aside class="gk-side">
    <div class="gk-side__card gk-side__promo">
      <p class="gk-side__promo-kicker">2026 智能报考季</p>
      <p class="gk-side__promo-title">AI 帮你定位目标院校</p>
      <button class="gk-side__promo-btn" type="button" @click="router.push({ path: '/agent', query: { q: '帮我定位目标院校，生成一份冲稳保志愿方案' } })">免费测一测</button>
    </div>
  </aside>
</template>
