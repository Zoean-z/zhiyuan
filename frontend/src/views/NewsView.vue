<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import { NEWS_ARTICLES } from "../utils/publicData";

const router = useRouter();
const activeTag = ref("全部");
const tags = ["全部", ...new Set(NEWS_ARTICLES.map((article) => article.tag))];
const articles = computed(() => activeTag.value === "全部" ? NEWS_ARTICLES : NEWS_ARTICLES.filter((article) => article.tag === activeTag.value));
</script>

<template>
  <div class="news-page">
    <GkHeader />
    <main class="news-shell">
      <header class="news-heading"><div><span>高考资讯</span><h1>重要时间与报考提醒</h1><p>摘要用于快速浏览，原文请以考试院或来源网站发布内容为准。</p></div></header>
      <nav class="news-tags" aria-label="资讯分类"><button v-for="tag in tags" :key="tag" type="button" :class="{ 'is-active': activeTag === tag }" @click="activeTag = tag">{{ tag }}</button></nav>
      <section class="news-list">
        <article v-for="article in articles" :key="article.id" @click="router.push(`/news/${article.id}`)">
          <div class="news-date"><strong>{{ article.date.slice(8) }}</strong><span>{{ article.date.slice(0, 7) }}</span></div>
          <div><span class="news-tag">{{ article.tag }}</span><h2>{{ article.title }}</h2><p>{{ article.summary }}</p><small>{{ article.source }} · 查看摘要与原文链接</small></div>
          <button type="button">查看详情</button>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.news-page { min-height: 100vh; background: #f7f7f8; color: #282b30; }.news-shell { width: min(980px, calc(100% - 40px)); margin: 0 auto; padding: 30px 0 54px; }.news-heading { padding: 30px 34px; border: 1px solid #eceff3; border-radius: 14px; background: #fff; }.news-heading span { color: #ff7a1a; font-size: 12px; font-weight: 700; }.news-heading h1 { margin: 8px 0 10px; font-size: 30px; }.news-heading p { margin: 0; color: #7d848e; }.news-tags { display: flex; gap: 8px; margin: 14px 0; padding: 12px 16px; border: 1px solid #eceff3; border-radius: 12px; background: #fff; }.news-tags button { min-width: 72px; min-height: 34px; border: 0; border-radius: 17px; background: transparent; color: #656c75; cursor: pointer; }.news-tags button.is-active { background: #ff7a1a; color: #fff; }.news-list { border: 1px solid #eceff3; border-radius: 14px; background: #fff; overflow: hidden; }.news-list article { display: grid; grid-template-columns: 72px minmax(0,1fr) max-content; gap: 24px; align-items: center; padding: 26px 28px; border-bottom: 1px solid #eff1f3; cursor: pointer; }.news-list article:last-child { border-bottom: 0; }.news-list article:hover { background: #fffbf7; }.news-date { display: grid; gap: 3px; text-align: center; }.news-date strong { color: #ff7a1a; font-size: 30px; }.news-date span { color: #969ca5; font-size: 12px; }.news-tag { color: #e96d17; font-size: 12px; font-weight: 700; }.news-list h2 { margin: 6px 0 9px; font-size: 19px; }.news-list p { margin: 0; color: #6f7680; line-height: 1.7; }.news-list small { display: block; margin-top: 10px; color: #a0a6ae; }.news-list article > button { padding: 8px 12px; border: 1px solid #ff7a1a; border-radius: 6px; background: #fff; color: #e96d17; cursor: pointer; }
@media (max-width: 680px) { .news-shell { width: calc(100% - 24px); }.news-list article { grid-template-columns: 52px 1fr; gap: 14px; padding: 22px 16px; }.news-list article > button { grid-column: 2; justify-self: start; }.news-heading { padding: 24px 20px; } }
</style>
