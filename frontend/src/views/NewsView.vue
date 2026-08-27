<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { CaretRight } from "@element-plus/icons-vue";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { NEWS_ARTICLES, NEWS_TAGS } from "../utils/newsData";

const router = useRouter();
const activeTag = ref("全部");

const filteredList = computed(() => {
  if (activeTag.value === "全部") return NEWS_ARTICLES;
  return NEWS_ARTICLES.filter((a) => a.tag === activeTag.value);
});
const tagCounts = computed(() => {
  const counts = { 全部: NEWS_ARTICLES.length };
  NEWS_ARTICLES.forEach((a) => {
    counts[a.tag] = (counts[a.tag] || 0) + 1;
  });
  return counts;
});

function openArticle(id) {
  router.push(`/news/${id}`);
}
function tagCls(tag) {
  return tag === "录取" ? "is-admit" : tag === "志愿" ? "is-wish" : "is-info";
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="高考资讯" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gk-news">
            <!-- 频道头 -->
            <header class="gk-news__head">
              <div>
                <nav class="gk-news__crumbs">
                  <span @click="router.push('/')">首页</span>
                  <i>/</i>
                  <em>高考资讯</em>
                </nav>
                <h2>高考资讯</h2>
                <p>招生录取动态 · 征集志愿通知 · 政策解读，内容整理自中国教育在线（掌上高考）</p>
              </div>
              <span class="gk-news__date">更新至 2026-08-14</span>
            </header>

            <!-- 分类 tab -->
            <div class="gk-news__tabs">
              <button
                v-for="t in NEWS_TAGS"
                :key="t"
                type="button"
                :class="{ 'is-active': activeTag === t }"
                @click="activeTag = t"
              >
                {{ t }} <i>{{ tagCounts[t] || 0 }}</i>
              </button>
            </div>

            <!-- 文章列表 -->
            <ul class="gk-news__list">
              <li v-for="a in filteredList" :key="a.id" @click="openArticle(a.id)">
                <span class="gk-news__tag" :class="tagCls(a.tag)">{{ a.tag }}</span>
                <div class="gk-news__info">
                  <h3>{{ a.title }}</h3>
                  <p>{{ a.summary }}</p>
                  <div class="gk-news__meta">
                    <span>{{ a.source }}</span>
                    <span>{{ a.dateFull }}</span>
                  </div>
                </div>
                <el-icon class="gk-news__arrow"><CaretRight /></el-icon>
              </li>
            </ul>

            <p v-if="!filteredList.length" class="gk-news__empty">该分类下暂无资讯</p>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
