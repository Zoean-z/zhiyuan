<script setup>
import { computed, ref } from "vue";
import { ChatDotRound, Collection, Document, OfficeBuilding, RefreshRight, Search } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import { MAJORS, NEWS_ARTICLES, selectSchoolShowcase } from "../utils/publicData";

const router = useRouter();
const directions = MAJORS.slice(0, 5);
const featuredNews = NEWS_ARTICLES[0];
const secondaryNews = NEWS_ARTICLES.slice(1, 5);
const schoolType = ref("全部");
const schoolOffset = ref(0);
const schoolTypes = ["全部", "综合类", "理工类"];
const showcaseSchools = computed(() => selectSchoolShowcase(schoolType.value, schoolOffset.value, 8));
const quickLinks = [
  { label: "查大学", hint: "按地区和类型浏览", path: "/schools", icon: OfficeBuilding },
  { label: "查专业", hint: "了解专业门类", path: "/majors", icon: Collection },
  { label: "志愿填报", hint: "分数或文本智能查询", path: "/recommend", icon: Search },
  { label: "志愿单", hint: "管理已选志愿", path: "/plans", icon: Document }
];

function formatMonthDay(date) {
  return String(date || "").slice(5);
}

function changeSchoolType(type) {
  schoolType.value = type;
  schoolOffset.value = 0;
}

function rotateSchools() {
  schoolOffset.value += 8;
}

function openSchool(school) {
  router.push({ path: "/schools", query: { q: school.name } });
}
</script>

<template>
  <div class="gk-page">
    <GkHeader />
    <main class="home-shell">
      <section class="home-hero">
        <div class="home-hero__content">
          <p class="home-eyebrow">2026 届智能报考季</p>
          <h1>让每一次志愿选择<br />都有依据</h1>
          <p>保留真实的分数查询与自然语言查询流程，从学校进入专业列表，再加入你的志愿单。</p>
          <div class="home-hero__actions">
            <button class="home-primary" type="button" @click="router.push('/recommend')">开始智能推荐</button>
            <button class="home-secondary" type="button" @click="router.push('/agent')">
              <el-icon><ChatDotRound /></el-icon> 问 AI 助手
            </button>
          </div>
        </div>

        <aside class="home-guide" aria-label="志愿填报流程">
          <span>填报路径</span>
          <ol>
            <li><b>01</b><div><strong>输入分数或需求</strong><small>使用现有推荐服务</small></div></li>
            <li><b>02</b><div><strong>查看院校与专业</strong><small>按冲、稳、保理解结果</small></div></li>
            <li><b>03</b><div><strong>加入并管理志愿单</strong><small>统一保存到后端方案</small></div></li>
          </ol>
          <button type="button" @click="router.push('/recommend')">进入志愿填报</button>
        </aside>
      </section>

      <nav class="home-quick" aria-label="快捷入口">
        <button v-for="item in quickLinks" :key="item.path" type="button" @click="router.push(item.path)">
          <span class="home-quick__icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span><strong>{{ item.label }}</strong><small>{{ item.hint }}</small></span>
        </button>
      </nav>

      <section class="hot-news-panel">
        <header class="home-section-title">
          <h2>热点资讯</h2><span>HOT</span>
          <button type="button" @click="router.push('/news')">更多资讯</button>
        </header>
        <button v-if="featuredNews" type="button" class="featured-news" @click="router.push(`/news/${featuredNews.id}`)">
          <em>{{ featuredNews.tag }}</em>
          <strong>{{ featuredNews.title }}</strong>
          <small>{{ featuredNews.source }} · {{ formatMonthDay(featuredNews.date) }}</small>
        </button>
        <div class="compact-news-list">
          <button v-for="article in secondaryNews" :key="article.id" type="button" @click="router.push(`/news/${article.id}`)">
            <i />
            <span>{{ article.title }}</span>
            <time>{{ formatMonthDay(article.date) }}</time>
          </button>
        </div>
      </section>

      <section class="popular-school-panel">
        <header class="popular-school-panel__head">
          <h2>热门院校</h2>
          <div class="popular-school-panel__tools">
            <div class="school-type-tabs" role="tablist" aria-label="院校类型">
              <button v-for="type in schoolTypes" :key="type" :class="{ 'is-active': schoolType === type }" @click="changeSchoolType(type)">{{ type }}</button>
            </div>
            <button class="school-rotate" type="button" @click="rotateSchools"><el-icon><RefreshRight /></el-icon>换一换</button>
          </div>
        </header>
        <div class="popular-school-grid">
          <button v-for="school in showcaseSchools" :key="school.id" type="button" class="popular-school-card" @click="openSchool(school)">
            <GkSchoolLogo :school="school" size="sm" />
            <span><strong>{{ school.name }}</strong><small>{{ school.type }} · {{ school.nature }}</small></span>
            <i>查看院校 &gt;</i>
          </button>
        </div>
      </section>

      <section class="home-panel home-directions-panel">
        <header class="home-panel__head">
          <div><span>专业探索</span><h2>从感兴趣的方向开始</h2></div>
          <button type="button" @click="router.push('/majors')">查看全部</button>
        </header>
        <div class="direction-list">
          <button v-for="major in directions" :key="major.code" type="button" @click="router.push({ path: '/majors', query: { q: major.name } })">
            <span class="direction-list__mark">{{ major.name.slice(0, 1) }}</span>
            <span><strong>{{ major.name }}</strong><small>{{ major.category }} · {{ major.sub }} · {{ major.duration }}</small></span>
            <i>查看</i>
          </button>
        </div>
      </section>
    </main>
    <footer class="gk-footer">智愿AI报考平台</footer>
  </div>
</template>

<style scoped>
.gk-page { min-height: 100vh; background: #f7f7f8; color: #24272c; }
.home-shell { width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 18px 0 48px; }
.home-hero { display: grid; grid-template-columns: minmax(0, 1.8fr) minmax(300px, .72fr); gap: 14px; }
.home-hero__content { min-height: 360px; padding: 58px 54px; border-radius: 16px; background: #3268e8; color: #fff; }
.home-eyebrow { display: inline-flex; margin: 0 0 18px; padding: 6px 12px; border: 1px solid rgba(255,255,255,.35); border-radius: 18px; font-size: 13px; }
.home-hero h1 { margin: 0; font-size: clamp(38px, 4vw, 58px); line-height: 1.16; letter-spacing: -2px; }
.home-hero__content > p:not(.home-eyebrow) { max-width: 620px; margin: 22px 0 0; color: rgba(255,255,255,.82); line-height: 1.8; }
.home-hero__actions { display: flex; gap: 12px; margin-top: 30px; }.home-hero__actions button { min-height: 44px; padding: 0 22px; border-radius: 24px; font-weight: 700; cursor: pointer; }
.home-primary { border: 0; background: #fff; color: #e96d17; }.home-secondary { display: flex; align-items: center; gap: 7px; border: 1px solid rgba(255,255,255,.55); background: transparent; color: #fff; }
.home-guide { padding: 28px; border: 1px solid #eceff3; border-radius: 16px; background: #fff; }.home-guide > span { color: #ff7a1a; font-size: 13px; font-weight: 700; }.home-guide ol { margin: 20px 0 24px; padding: 0; list-style: none; }.home-guide li { display: flex; gap: 14px; padding: 14px 0; border-bottom: 1px solid #eff1f4; }.home-guide li b { color: #ff7a1a; font-size: 13px; }.home-guide li div { display: grid; gap: 4px; }.home-guide li strong { font-size: 15px; }.home-guide li small { color: #8a9099; }.home-guide > button { width: 100%; min-height: 44px; border: 0; border-radius: 8px; background: #ff7a1a; color: #fff; font-weight: 700; cursor: pointer; }
.home-quick { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 14px; border: 1px solid #eceff3; border-radius: 14px; background: #fff; }.home-quick button { display: flex; align-items: center; gap: 13px; padding: 20px 24px; border: 0; border-right: 1px solid #eff1f4; background: transparent; text-align: left; cursor: pointer; }.home-quick button:last-child { border-right: 0; }.home-quick button:hover { background: #fff8f2; }.home-quick__icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 10px; background: #fff0e4; color: #ff7a1a; font-size: 20px; }.home-quick button > span:last-child { display: grid; gap: 4px; }.home-quick small { color: #9298a1; }
.hot-news-panel,.popular-school-panel,.home-panel { margin-top: 14px; padding: 24px 26px; border: 1px solid #eceff3; border-radius: 14px; background: #fff; }
.home-section-title { display: flex; align-items: center; gap: 10px; }.home-section-title h2,.popular-school-panel h2 { margin: 0; color: #152033; font-size: 22px; }.home-section-title > span { padding: 3px 10px; border-radius: 6px; background: #ff6434; color: #fff; font-size: 12px; font-weight: 800; letter-spacing: .5px; }.home-section-title > button { margin-left: auto; border: 0; background: transparent; color: #e96d17; cursor: pointer; }
.featured-news { display: grid; justify-items: start; gap: 8px; width: 100%; padding: 18px 0 16px; border: 0; border-bottom: 1px dashed #e1e5eb; background: transparent; text-align: left; cursor: pointer; }.featured-news em { padding: 4px 10px; border-radius: 6px; background: #fff0e5; color: #ff650f; font-style: normal; font-size: 12px; }.featured-news strong { color: #131d30; font-size: 20px; line-height: 1.45; }.featured-news small { color: #9aa4b5; font-size: 13px; }
.compact-news-list { display: grid; }.compact-news-list button { display: grid; grid-template-columns: 10px minmax(0,1fr) max-content; align-items: center; gap: 10px; min-height: 42px; padding: 0; border: 0; background: transparent; color: #344156; text-align: left; cursor: pointer; }.compact-news-list i { width: 7px; height: 7px; border-radius: 50%; background: #d7dde6; }.compact-news-list span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.compact-news-list time { color: #a3abb9; }
.popular-school-panel__head { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.popular-school-panel__tools { display: flex; align-items: center; gap: 12px; }.school-type-tabs { display: flex; padding: 4px; border-radius: 10px; background: #f4f5f7; }.school-type-tabs button { min-height: 36px; padding: 0 18px; border: 0; border-radius: 8px; background: transparent; color: #5f6878; cursor: pointer; }.school-type-tabs button.is-active { background: #fff; color: #ff650f; font-weight: 700; box-shadow: 0 2px 8px rgba(30,41,59,.08); }.school-rotate { display: inline-flex; align-items: center; gap: 5px; min-height: 38px; padding: 0 17px; border: 1px solid #ffc79f; border-radius: 21px; background: #fff; color: #ff650f; cursor: pointer; }
.popular-school-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 12px; }.popular-school-card { display: grid; grid-template-columns: max-content minmax(0,1fr) max-content; align-items: center; gap: 12px; min-width: 0; min-height: 96px; padding: 14px 16px; border: 1px solid #e8ebf0; border-radius: 13px; background: #fff; color: inherit; text-align: left; cursor: pointer; }.popular-school-card:hover { border-color: #ffc49b; background: #fffaf6; }.popular-school-card > span { display: grid; gap: 5px; min-width: 0; }.popular-school-card strong { overflow: hidden; color: #172033; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }.popular-school-card small { color: #8993a4; }.popular-school-card > i { color: #a6aebb; font-style: normal; font-size: 12px; }
.home-panel__head { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 16px; }.home-panel__head span { color: #ff7a1a; font-size: 12px; font-weight: 700; }.home-panel__head h2 { margin: 5px 0 0; font-size: 20px; }.home-panel__head button { border: 0; background: transparent; color: #e96d17; cursor: pointer; }.direction-list { display: grid; grid-template-columns: repeat(5, minmax(0,1fr)); gap: 10px; }.direction-list button { display: grid; grid-template-columns: 38px minmax(0,1fr); align-items: center; gap: 10px; min-width: 0; min-height: 72px; padding: 10px; border: 1px solid #edf0f3; border-radius: 10px; background: #fff; text-align: left; cursor: pointer; }.direction-list__mark { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 9px; background: #fff1e7; color: #ff7a1a; font-weight: 700; }.direction-list button > span:nth-child(2) { display: grid; gap: 4px; min-width: 0; }.direction-list strong,.direction-list small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.direction-list small { color: #8f959e; font-size: 11px; }.direction-list button > i { display: none; }
.gk-footer { padding: 28px 20px; border-top: 1px solid #e9ebee; background: #fff; color: #8a9099; text-align: center; font-size: 13px; }
@media (max-width: 1000px) { .popular-school-grid { grid-template-columns: repeat(2, minmax(0,1fr)); }.direction-list { grid-template-columns: repeat(2, minmax(0,1fr)); } }
@media (max-width: 900px) { .home-hero { grid-template-columns: 1fr; }.home-quick { grid-template-columns: repeat(2, 1fr); }.home-quick button:nth-child(2) { border-right: 0; }.home-hero__content { min-height: 310px; padding: 42px 34px; }.popular-school-panel__head { align-items: flex-start; flex-direction: column; } }
@media (max-width: 560px) { .home-shell { width: calc(100% - 24px); }.home-quick { grid-template-columns: 1fr; }.home-quick button { border-right: 0; border-bottom: 1px solid #eff1f4; }.home-hero__content { padding: 34px 24px; }.home-hero__actions { align-items: stretch; flex-direction: column; }.hot-news-panel,.popular-school-panel,.home-panel { padding: 20px 16px; }.featured-news strong { font-size: 17px; }.compact-news-list button { min-height: 52px; }.popular-school-panel__tools { align-items: stretch; flex-direction: column; width: 100%; }.school-type-tabs button { flex: 1; padding: 0 10px; }.school-rotate { justify-content: center; }.popular-school-grid,.direction-list { grid-template-columns: 1fr; }.popular-school-card { grid-template-columns: max-content minmax(0,1fr); }.popular-school-card > i { grid-column: 2; }.home-section-title > button { font-size: 12px; } }
</style>
