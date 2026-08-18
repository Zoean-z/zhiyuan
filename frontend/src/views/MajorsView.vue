<script setup>
import { Search } from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";

const route = useRoute();
const router = useRouter();
const categories = ["全部", "工学", "理学", "医学", "文学", "管理学", "经济学", "法学", "教育学", "艺术学", "农学", "哲学", "历史学"];
const keyword = ref(String(route.query.q || ""));
const category = ref("全部");
const majors = ref([]);
const loading = ref(false);
const error = ref("");

const filteredMajors = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  return majors.value.filter((major) => {
    const matchesCategory = category.value === "全部" || major.category === category.value;
    const matchesKeyword = !q || [major.name, major.code, major.category, major.subcategory]
      .some((value) => String(value || "").toLowerCase().includes(q));
    return matchesCategory && matchesKeyword;
  });
});

watch(() => route.query.q, (value) => { keyword.value = String(value || ""); });

async function loadMajors() {
  loading.value = true;
  error.value = "";
  try {
    const response = await fetch("/api/explore/majors");
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    majors.value = await response.json();
  } catch (cause) {
    error.value = "专业数据暂时无法加载，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function submitSearch() {
  const q = keyword.value.trim();
  router.replace({ path: "/majors", query: q ? { q } : {} });
}

function openMajor(major, tab = "intro") {
  router.push({ path: `/majors/${major.code}`, query: tab === "schools" ? { tab: "schools" } : {} });
}

function askMajor(major) {
  router.push({ path: "/agent", query: { q: `${major.name}专业的学习内容和就业方向是什么？` } });
}

onMounted(loadMajors);
</script>

<template>
  <div class="major-explore-page">
    <GkHeader />
    <main class="major-explore-shell">
      <section class="major-explore-search">
        <div><span>专业探索</span><h1>查专业</h1></div>
        <form role="search" @submit.prevent="submitSearch">
          <el-icon><Search /></el-icon>
          <input v-model="keyword" type="search" placeholder="输入专业名称或专业代码" aria-label="搜索专业" />
          <button type="submit">搜索</button>
        </form>
      </section>

      <section class="major-explore-body">
        <aside class="major-category-panel">
          <h2>热门门类</h2>
          <button v-for="item in categories" :key="item" type="button" :class="{ 'is-active': category === item }" @click="category = item">
            <span>{{ item }}</span><i>›</i>
          </button>
        </aside>

        <div class="major-result-panel">
          <header><span>{{ !keyword && category === '全部' ? '热门专业' : `共 ${filteredMajors.length} 个专业` }}</span></header>
          <div v-if="loading" class="major-state">正在加载专业数据…</div>
          <div v-else-if="error" class="major-state major-state--error">{{ error }}<button type="button" @click="loadMajors">重新加载</button></div>
          <ul v-else class="major-reference-list">
            <li v-for="major in filteredMajors" :key="major.code" @click="openMajor(major)">
              <div class="major-reference-info">
                <p><strong>{{ major.name }}</strong><span>{{ major.code }}</span></p>
                <p><span>修业年限：{{ major.duration }}</span><span>授予学位：{{ major.degree }}</span><span v-if="major.genderRatio">男女比例：{{ major.genderRatio }}</span><span v-if="major.averageSalary">平均年薪：¥{{ major.averageSalary }}</span></p>
                <small>{{ major.category }} · {{ major.subcategory }} · {{ major.offeringSchoolCount }} 所院校开设</small>
              </div>
              <div class="major-reference-actions">
                <button type="button" class="is-ghost" @click.stop="askMajor(major)">问前景</button>
                <button type="button" class="is-primary" @click.stop="openMajor(major, 'schools')">开设院校 &gt;</button>
              </div>
            </li>
            <li v-if="!filteredMajors.length" class="major-state">没有符合条件的专业，请调整门类或关键词。</li>
          </ul>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.major-explore-page { min-height: 100vh; background: #f7f7f8; color: #20242b; }
.major-explore-shell { width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 18px 0 52px; }
.major-explore-search { display: grid; grid-template-columns: max-content minmax(320px, 620px); align-items: end; justify-content: space-between; gap: 18px; padding: 24px 28px; border: 1px solid #eceff3; border-radius: 14px; background: #fff; }
.major-explore-search span { color: #ff7218; font-size: 12px; font-weight: 700; }.major-explore-search h1 { margin: 5px 0 0; font-size: 26px; }
.major-explore-search form { height: 44px; display: grid; grid-template-columns: 42px 1fr 88px; align-items: center; overflow: hidden; border: 1px solid #ff9a55; border-radius: 8px; color: #a1a7af; }.major-explore-search form .el-icon { justify-self: end; }.major-explore-search input { min-width: 0; height: 100%; padding: 0 12px; border: 0; outline: 0; font: inherit; }.major-explore-search form button { align-self: stretch; border: 0; background: #ff7a1a; color: #fff; font-weight: 700; cursor: pointer; }
.major-explore-search > p { grid-column: 1 / -1; margin: 0; color: #8d6a50; font-size: 12px; }
.major-explore-body { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 20px; margin-top: 16px; }
.major-category-panel,.major-result-panel { border: 1px solid #e8eaee; border-radius: 14px; background: #fff; }.major-category-panel { align-self: start; padding: 24px 16px; }.major-category-panel h2 { margin: 0 12px 12px; padding-bottom: 16px; border-bottom: 1px solid #eef0f2; font-size: 20px; }.major-category-panel button { width: 100%; min-height: 48px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; border: 0; border-radius: 7px; background: transparent; color: #363b43; font: inherit; font-size: 16px; cursor: pointer; }.major-category-panel button i { color: #a9afb8; font-style: normal; }.major-category-panel button:hover,.major-category-panel button.is-active { color: #f06a17; background: #fff5ee; font-weight: 700; }
.major-result-panel { overflow: hidden; }.major-result-panel > header { min-height: 54px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0 26px; border-bottom: 1px solid #eceef1; color: #6f757e; }.major-result-panel > header small { color: #9aa0a8; }
.major-reference-list { margin: 0; padding: 0; list-style: none; }.major-reference-list > li { min-height: 166px; display: grid; grid-template-columns: minmax(0, 1fr) max-content; align-items: center; gap: 24px; padding: 24px 28px; border-bottom: 1px solid #eceef1; cursor: pointer; }.major-reference-list > li:last-child { border-bottom: 0; }.major-reference-list > li:hover { background: #fffbf8; }.major-reference-info { min-width: 0; }.major-reference-info > p:first-child { display: flex; align-items: center; gap: 12px; margin: 0; }.major-reference-info strong { font-size: 22px; }.major-reference-info > p:first-child span { padding: 3px 10px; border-radius: 15px; background: #f2f3f5; color: #8a919b; }
.major-reference-info > p:nth-child(2) { display: flex; flex-wrap: wrap; gap: 8px 24px; margin: 11px 0 8px; color: #656c76; font-size: 15px; }.major-reference-info small { color: #9ba1aa; font-size: 14px; }.major-reference-actions { display: flex; gap: 12px; }.major-reference-actions button { min-height: 42px; padding: 0 18px; border-radius: 9px; font-weight: 700; cursor: pointer; }.major-reference-actions .is-ghost { border: 1px solid #ffad79; background: #fff; color: #f06b18; }.major-reference-actions .is-primary { border: 0; background: #ff7a1a; color: #fff; }
.major-state { padding: 72px 24px; color: #8a9098; text-align: center; }.major-state--error button { margin-left: 10px; border: 0; background: transparent; color: #f06b18; cursor: pointer; }
@media (max-width: 900px) { .major-explore-search { grid-template-columns: 1fr; align-items: stretch; }.major-explore-body { grid-template-columns: 1fr; }.major-category-panel { display: flex; gap: 6px; overflow-x: auto; padding: 12px; }.major-category-panel h2 { display: none; }.major-category-panel button { width: auto; flex: 0 0 auto; min-height: 38px; }.major-category-panel button i { display: none; } }
@media (max-width: 640px) { .major-explore-shell { width: calc(100% - 24px); }.major-explore-search { padding: 18px; }.major-reference-list > li { grid-template-columns: 1fr; padding: 20px; }.major-reference-actions { width: 100%; }.major-reference-actions button { flex: 1; }.major-reference-info > p:nth-child(2) { display: grid; gap: 6px; } }
</style>
