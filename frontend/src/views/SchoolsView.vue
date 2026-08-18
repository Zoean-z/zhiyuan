<script setup>
import { Search } from "@element-plus/icons-vue";
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import { SCHOOLS, SCHOOL_PROVINCES, SCHOOL_TYPES } from "../utils/publicData";

const route = useRoute();
const router = useRouter();
const keyword = ref(String(route.query.q || ""));
const province = ref("全部");
const type = ref("全部");
const level = ref("全部");

watch(() => route.query.q, (value) => { keyword.value = String(value || ""); });

const schools = computed(() => SCHOOLS.filter((school) => {
  const q = keyword.value.trim().toLowerCase();
  const matchesKeyword = !q || [school.name, school.province, school.city, school.type].some((value) => value.toLowerCase().includes(q));
  const matchesProvince = province.value === "全部" || school.province === province.value;
  const matchesType = type.value === "全部" || school.type === type.value;
  const matchesLevel = level.value === "全部" || (level.value === "985" && school.is985) || (level.value === "211" && school.is211) || (level.value === "双一流" && school.isDoubleFirstClass);
  return matchesKeyword && matchesProvince && matchesType && matchesLevel;
}));

function search() {
  router.replace({ path: "/schools", query: keyword.value.trim() ? { q: keyword.value.trim() } : {} });
}
</script>

<template>
  <div class="gk-list-page">
    <GkHeader />
    <main class="gk-list-shell">
      <section class="gk-filter-card">
        <div class="gk-filter-card__title"><span>查大学</span><h1>浏览院校基础信息</h1></div>
        <div class="gk-filter-row">
          <el-select v-model="province" aria-label="按省份筛选"><el-option v-for="item in SCHOOL_PROVINCES" :key="item" :label="item === '全部' ? '全部地区' : item" :value="item" /></el-select>
          <el-select v-model="type" aria-label="按类型筛选"><el-option v-for="item in SCHOOL_TYPES" :key="item" :label="item === '全部' ? '全部类型' : item" :value="item" /></el-select>
          <el-select v-model="level" aria-label="按层次筛选"><el-option v-for="item in ['全部', '985', '211', '双一流']" :key="item" :label="item === '全部' ? '全部层次' : item" :value="item" /></el-select>
        </div>
        <form class="gk-filter-search" role="search" @submit.prevent="search">
          <el-icon><Search /></el-icon><input v-model="keyword" placeholder="输入院校名称" /><button type="submit">搜索</button>
        </form>
      </section>

      <section class="gk-school-results">
        <header><span>找到 <strong>{{ schools.length }}</strong> 所院校</span></header>
        <div v-if="schools.length" class="gk-school-list">
          <article v-for="school in schools" :key="school.id" class="gk-school-row">
            <GkSchoolLogo :school="school" />
            <div class="gk-school-row__main">
              <h2>{{ school.name }} <small>{{ school.province }}{{ school.city === school.province ? '' : school.city }}</small></h2>
              <p>本科 · {{ school.type }} · {{ school.nature }} · {{ school.belong }}</p>
              <div><span v-if="school.is985">985</span><span v-if="school.is211">211</span><span v-if="school.isDoubleFirstClass">双一流</span></div>
            </div>
            <button type="button" @click="router.push('/recommend')">进入志愿填报</button>
          </article>
        </div>
        <div v-else class="gk-empty">没有符合条件的院校，请调整筛选条件。</div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.gk-list-page { min-height: 100vh; background: #f7f7f8; color: #26292e; }
.gk-list-shell { width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 18px 0 48px; }
.gk-filter-card, .gk-school-results { border: 1px solid #eceff3; border-radius: 14px; background: #fff; }
.gk-filter-card { padding: 24px; }
.gk-filter-card__title span { color: #ff7a1a; font-size: 12px; font-weight: 700; }
.gk-filter-card__title h1 { margin: 5px 0 20px; font-size: 24px; }
.gk-filter-row { display: grid; grid-template-columns: repeat(3, minmax(0, 180px)); gap: 10px; }
.gk-filter-search { max-width: 760px; height: 42px; display: grid; grid-template-columns: 40px 1fr 88px; align-items: center; margin-top: 12px; border: 1px solid #ff9d5b; border-radius: 7px; overflow: hidden; color: #9da3ac; }
.gk-filter-search .el-icon { justify-self: end; }
.gk-filter-search input { min-width: 0; height: 100%; padding: 0 12px; border: 0; outline: 0; font: inherit; }
.gk-filter-search button { align-self: stretch; border: 0; background: #ff7a1a; color: #fff; font-weight: 700; cursor: pointer; }
.gk-filter-card > p { margin: 14px 0 0; color: #8a6a53; font-size: 12px; }
.gk-school-results { margin-top: 14px; overflow: hidden; }
.gk-school-results > header { min-height: 56px; display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 0 24px; border-bottom: 1px solid #eff1f3; }
.gk-school-results > header strong { color: #ff7a1a; }.gk-school-results > header small { color: #9298a1; }
.gk-school-row { min-height: 132px; display: grid; grid-template-columns: max-content minmax(0, 1fr) max-content; align-items: center; gap: 22px; padding: 24px; border-bottom: 1px solid #eff1f3; }
.gk-school-row:last-child { border-bottom: 0; }
.gk-school-row:hover { background: #fffbf7; }
.gk-school-row__main { min-width: 0; }
.gk-school-row h2 { margin: 0; font-size: 21px; }.gk-school-row h2 small { margin-left: 8px; color: #9aa0a8; font-size: 13px; font-weight: 400; }
.gk-school-row p { margin: 9px 0; color: #737a84; font-size: 14px; }
.gk-school-row__main div { display: flex; flex-wrap: wrap; gap: 6px; }.gk-school-row__main div span { padding: 3px 7px; border-radius: 4px; background: #f2f3f5; color: #666e78; font-size: 12px; }
.gk-school-row > button { min-height: 38px; padding: 0 15px; border: 1px solid #ff7a1a; border-radius: 7px; background: #fff; color: #e96d17; cursor: pointer; }.gk-school-row > button:hover { background: #ff7a1a; color: #fff; }
.gk-empty { padding: 70px 24px; color: #8e949c; text-align: center; }
@media (max-width: 760px) { .gk-list-shell { width: calc(100% - 24px); }.gk-filter-row { grid-template-columns: 1fr; }.gk-school-row { grid-template-columns: max-content 1fr; gap: 15px; }.gk-school-row > button { grid-column: 1 / -1; }.gk-school-results > header small { display: none; } }
</style>
