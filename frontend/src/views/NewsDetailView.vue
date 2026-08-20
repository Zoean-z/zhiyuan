<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { CaretRight, Link as LinkIcon } from "@element-plus/icons-vue";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { NEWS_ARTICLES, newsById, viewsOf } from "../utils/newsData";

const route = useRoute();
const router = useRouter();

const article = computed(() => newsById(route.params.id));

const related = computed(() => {
  if (!article.value) return [];
  const sameTag = NEWS_ARTICLES.filter((a) => a.id !== article.value.id && a.tag === article.value.tag);
  const others = NEWS_ARTICLES.filter((a) => a.id !== article.value.id && a.tag !== article.value.tag);
  return [...sameTag, ...others].slice(0, 5);
});

// 以「一、二、…」开头的段落渲染为小标题
function isHeading(p) {
  return /^[一二三四五六七八九十]、|温馨提示|温馨提醒/.test(p);
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
          <!-- 文章不存在 -->
          <div v-if="!article" class="gk-nd__missing">
            <p>资讯不存在或已下线</p>
            <button type="button" @click="router.push('/news')">返回资讯频道</button>
          </div>

          <article v-else class="gk-nd">
            <nav class="gk-nd__crumbs">
              <span @click="router.push('/')">首页</span>
              <i>/</i>
              <span @click="router.push('/news')">高考资讯</span>
              <i>/</i>
              <em>正文</em>
            </nav>

            <h1 class="gk-nd__title">{{ article.title }}</h1>

            <div class="gk-nd__meta">
              <span class="gk-nd__tag" :class="tagCls(article.tag)">{{ article.tag }}</span>
              <span>来源：{{ article.source }}（中国教育在线）</span>
              <span>{{ article.dateFull }}</span>
              <span>阅读 {{ viewsOf(article.id).toLocaleString("zh-CN") }}</span>
              <a
                class="gk-nd__origin"
                :href="article.url"
                target="_blank"
                rel="noopener noreferrer"
              >
                查看原文 <el-icon><LinkIcon /></el-icon>
              </a>
            </div>

            <div class="gk-nd__body">
              <p
                v-for="(p, i) in article.content"
                :key="i"
                :class="{ 'gk-nd__h': isHeading(p) }"
              >
                {{ p }}
              </p>
            </div>

            <div class="gk-nd__foot">
              <p class="gk-nd__notice">
                本文内容整理自中国教育在线（掌上高考），仅供志愿填报参考；具体政策与时间安排以各省教育考试院官方发布为准。
              </p>
              <div class="gk-nd__actions">
                <button type="button" class="gk-nd__ask" @click="router.push({ path: '/agent', query: { q: `帮我解读这条资讯：${article.title}` } })">
                  问小智解读
                </button>
                <a class="gk-nd__origin-btn" :href="article.url" target="_blank" rel="noopener noreferrer">
                  前往中国教育在线查看原文
                </a>
              </div>
            </div>

            <!-- 相关阅读 -->
            <section class="gk-nd__related">
              <h4>相关阅读</h4>
              <ul>
                <li v-for="r in related" :key="r.id" @click="router.push(`/news/${r.id}`)">
                  <el-icon><CaretRight /></el-icon>
                  <span>{{ r.title }}</span>
                  <em>{{ r.date }}</em>
                </li>
              </ul>
            </section>
          </article>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
