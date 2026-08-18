<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import { newsById } from "../utils/publicData";

const route = useRoute();
const router = useRouter();
const article = computed(() => newsById(route.params.id));
</script>

<template>
  <div class="news-detail-page">
    <GkHeader />
    <main class="news-detail-shell">
      <button class="news-back" type="button" @click="router.push('/news')">← 返回资讯列表</button>
      <article v-if="article">
        <span>{{ article.tag }}</span>
        <h1>{{ article.title }}</h1>
        <p class="news-detail-meta">{{ article.date }} · {{ article.source }}</p>
        <p class="news-detail-summary">{{ article.summary }}</p>
        <div class="news-detail-note">本页仅提供原创摘要，不复制第三方正文。招生政策和时间安排可能调整，请打开来源网站并结合当地考试院公告核验。</div>
        <a :href="article.url" target="_blank" rel="noopener noreferrer">打开来源原文</a>
      </article>
      <div v-else class="news-missing">未找到该资讯。<button type="button" @click="router.push('/news')">返回列表</button></div>
    </main>
  </div>
</template>

<style scoped>
.news-detail-page { min-height: 100vh; background: #f7f7f8; color: #282b30; }.news-detail-shell { width: min(860px, calc(100% - 40px)); margin: 0 auto; padding: 28px 0 60px; }.news-back { margin-bottom: 14px; border: 0; background: transparent; color: #e96d17; cursor: pointer; }.news-detail-shell article { padding: 42px 48px; border: 1px solid #eceff3; border-radius: 14px; background: #fff; }.news-detail-shell article > span { color: #ff7a1a; font-size: 13px; font-weight: 700; }.news-detail-shell h1 { margin: 10px 0 14px; font-size: 32px; line-height: 1.4; }.news-detail-meta { color: #969ca5; }.news-detail-summary { margin-top: 30px; font-size: 17px; line-height: 1.9; }.news-detail-note { margin: 28px 0; padding: 16px; border-left: 3px solid #ff7a1a; background: #fff8f2; color: #79583f; line-height: 1.7; }.news-detail-shell a { display: inline-flex; padding: 11px 18px; border-radius: 7px; background: #ff7a1a; color: #fff; text-decoration: none; font-weight: 700; }.news-missing { padding: 70px 20px; text-align: center; }.news-missing button { border: 0; background: transparent; color: #e96d17; cursor: pointer; }
@media (max-width: 600px) { .news-detail-shell { width: calc(100% - 24px); }.news-detail-shell article { padding: 28px 22px; }.news-detail-shell h1 { font-size: 25px; } }
</style>
