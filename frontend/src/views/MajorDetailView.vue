<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";

const route = useRoute();
const router = useRouter();
const tabs = [{ key: "intro", label: "专业概况" }, { key: "schools", label: "开设院校" }, { key: "jobs", label: "相关就业" }];
const activeTab = ref(tabs.some((tab) => tab.key === route.query.tab) ? route.query.tab : "intro");
const detail = ref(null);
const schools = ref([]);
const loading = ref(false);
const error = ref("");
const province = ref("全部");
const type = ref("全部");
const nature = ref("全部");
const feature = ref("全部");

const major = computed(() => detail.value?.major || null);
const provinces = computed(() => ["全部", ...new Set(schools.value.map((school) => school.province))]);
const types = computed(() => ["全部", ...new Set(schools.value.map((school) => school.type))]);
const filteredSchools = computed(() => schools.value.filter((school) => {
  if (province.value !== "全部" && school.province !== province.value) return false;
  if (type.value !== "全部" && school.type !== type.value) return false;
  if (nature.value !== "全部" && school.nature !== nature.value) return false;
  if (feature.value === "985" && !school.is985) return false;
  if (feature.value === "211" && !school.is211) return false;
  if (feature.value === "双一流" && !school.isDoubleFirstClass) return false;
  return true;
}));

async function loadDetail() {
  loading.value = true;
  error.value = "";
  try {
    const code = encodeURIComponent(String(route.params.code || ""));
    const [detailResponse, schoolResponse] = await Promise.all([fetch(`/api/explore/majors/${code}`), fetch(`/api/explore/majors/${code}/schools`)]);
    if (!detailResponse.ok || !schoolResponse.ok) throw new Error("load failed");
    detail.value = await detailResponse.json();
    schools.value = await schoolResponse.json();
  } catch (cause) {
    error.value = "专业详情暂时无法加载，请返回专业列表重试。";
  } finally {
    loading.value = false;
  }
}

function switchTab(key) { activeTab.value = key; router.replace({ path: route.path, query: key === "intro" ? {} : { tab: key } }); }
function askEmployment() { if (major.value) router.push({ path: "/agent", query: { q: `${major.value.name}专业的就业方向和学习建议是什么？` } }); }
watch(() => route.query.tab, (tab) => { activeTab.value = tabs.some((item) => item.key === tab) ? tab : "intro"; });
onMounted(loadDetail);
</script>

<template>
  <div class="major-detail-page">
    <GkHeader />
    <main class="major-detail-shell">
      <div v-if="loading" class="major-detail-state">正在加载专业详情…</div>
      <div v-else-if="error" class="major-detail-state">{{ error }} <button type="button" @click="router.push('/majors')">返回查专业</button></div>
      <template v-else-if="major">
        <section class="major-detail-head">
          <div><h1>{{ major.name }}</h1><span v-if="major.popularity">人气值 {{ Number(major.popularity).toLocaleString() }}</span></div>
          <p><i>本科（普通）</i><i>{{ major.category }}</i><i>{{ major.subcategory }}</i></p>
        </section>

        <section class="major-detail-card">
          <nav class="major-detail-tabs" aria-label="专业详情导航"><button v-for="tab in tabs" :key="tab.key" type="button" :class="{ 'is-active': activeTab === tab.key }" @click="switchTab(tab.key)">{{ tab.label }}</button></nav>
          <div v-if="activeTab === 'intro'" class="major-detail-panel">
            <dl class="major-facts"><div><dt>修业年限</dt><dd>{{ major.duration }}</dd></div><div><dt>授予学位</dt><dd>{{ major.degree }}</dd></div><div><dt>男女比例</dt><dd>{{ major.genderRatio || '暂无' }}</dd></div><div><dt>平均年薪</dt><dd>{{ major.averageSalary ? `¥${major.averageSalary}` : '暂无' }}</dd></div><div><dt>已录入院校</dt><dd>{{ major.offeringSchoolCount }} 所</dd></div><div><dt>专业代码</dt><dd>{{ major.code }}</dd></div></dl>
            <h2>专业介绍</h2><p class="major-detail-copy">{{ detail.description }}</p><h2>就业方向</h2><div class="major-job-tags"><span v-for="item in detail.employmentDirections" :key="item">{{ item }}</span></div><button class="major-detail-cta" type="button" @click="askEmployment">让小智分析就业前景 &gt;</button>
          </div>

          <div v-else-if="activeTab === 'schools'" class="major-detail-panel major-school-panel">
            <div class="major-school-filters">
              <el-select v-model="province" aria-label="院校所属"><el-option v-for="item in provinces" :key="item" :label="item === '全部' ? '院校所属' : item" :value="item" /></el-select>
              <el-select v-model="type" aria-label="院校类型"><el-option v-for="item in types" :key="item" :label="item === '全部' ? '院校类型' : item" :value="item" /></el-select>
              <el-select v-model="nature" aria-label="办学类型"><el-option v-for="item in ['全部','公办','民办','中外合作办学']" :key="item" :label="item === '全部' ? '办学类型' : item" :value="item" /></el-select>
              <el-select v-model="feature" aria-label="院校特色"><el-option v-for="item in ['全部','985','211','双一流']" :key="item" :label="item === '全部' ? '院校特色' : item" :value="item" /></el-select>
            </div>
            <header><span>院校 <strong>{{ filteredSchools.length }}</strong> 所</span></header>
            <ul class="major-school-list">
              <li v-for="school in filteredSchools" :key="school.id"><GkSchoolLogo :school="school" /><div><h3>{{ school.name }}</h3><p>{{ school.province }}{{ school.city && school.city !== school.province ? ` · ${school.city}` : '' }} · {{ school.type }} · {{ school.nature }}</p><span v-if="school.is985">985</span><span v-if="school.is211">211</span><span v-if="school.isDoubleFirstClass">双一流</span></div></li>
              <li v-if="!filteredSchools.length" class="major-detail-state">没有符合筛选条件的院校。</li>
            </ul>
          </div>

          <div v-else class="major-detail-panel">
            <div class="major-job-summary"><div><strong>{{ major.averageSalary || '暂无' }}</strong><span>平均年薪</span></div><div><strong>{{ major.genderRatio || '暂无' }}</strong><span>男女比例</span></div><div><strong>{{ major.offeringSchoolCount }}</strong><span>已录入院校数</span></div></div><h2>主要就业去向</h2><div class="major-job-tags"><span v-for="item in detail.employmentDirections" :key="item">{{ item }}</span></div><button class="major-detail-cta" type="button" @click="askEmployment">问问小智：{{ major.name }}好就业吗 &gt;</button>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.major-detail-page { min-height: 100vh; background: #f5f6f8; color: #20242b; }.major-detail-shell { width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 24px 0 52px; }.major-detail-head,.major-detail-card,.major-detail-state { border: 1px solid #ebeef1; border-radius: 16px; background: #fff; }.major-detail-head { padding: 28px 34px; }.major-detail-head > div { display: flex; align-items: center; gap: 18px; }.major-detail-head h1 { margin: 0; font-size: 30px; }.major-detail-head > div span { padding: 8px 16px; border-radius: 20px; background: #fff0e8; color: #ff5e45; }.major-detail-head p { display: flex; flex-wrap: wrap; gap: 8px; margin: 13px 0 8px; }.major-detail-head p i { padding: 4px 9px; border-radius: 4px; background: #f3f4f6; color: #68707a; font-style: normal; }.major-detail-head small { color: #9aa0a8; }
.major-detail-card { margin-top: 18px; overflow: hidden; }.major-detail-tabs { min-height: 72px; display: grid; grid-template-columns: repeat(3, 1fr); border-bottom: 1px solid #eceef1; }.major-detail-tabs button { border: 0; background: #fff; color: #24272d; font: inherit; font-size: 20px; cursor: pointer; }.major-detail-tabs button.is-active { background: #ff7a1a; color: #fff; }.major-detail-panel { padding: 28px; }.major-facts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin: 0; }.major-facts div { display: flex; align-items: center; gap: 18px; min-height: 72px; padding: 0 20px; border-radius: 10px; background: #f8f9fa; }.major-facts dt { color: #9198a2; }.major-facts dd { margin: 0; font-size: 17px; }.major-detail-panel h2 { margin: 28px 0 14px; padding-left: 12px; border-left: 4px solid #ff7218; font-size: 20px; }.major-detail-copy { color: #5d6570; line-height: 1.9; }.major-job-tags { display: flex; flex-wrap: wrap; gap: 10px; }.major-job-tags span { padding: 9px 18px; border-radius: 22px; background: #f4f5f6; color: #414852; }.major-detail-cta { margin-top: 28px; min-height: 46px; padding: 0 22px; border: 0; border-radius: 24px; background: #ff7a1a; color: #fff; font-weight: 700; cursor: pointer; }
.major-school-filters { display: grid; grid-template-columns: repeat(4, minmax(0, 180px)); gap: 14px; padding-bottom: 20px; border-bottom: 1px solid #eceef1; }.major-school-panel > header { display: flex; justify-content: space-between; gap: 18px; padding: 20px 0 10px; color: #66707b; }.major-school-panel > header strong { color: #ff6d18; }.major-school-panel > header small { color: #999fa8; }.major-school-list { margin: 0; padding: 0; list-style: none; }.major-school-list > li { min-height: 142px; display: flex; align-items: center; gap: 22px; padding: 22px 6px; border-bottom: 1px solid #eceef1; }.major-school-list h3 { margin: 0 0 8px; font-size: 21px; }.major-school-list p { margin: 0 0 9px; color: #7a818b; }.major-school-list div > span { display: inline-flex; margin-right: 6px; padding: 4px 8px; border-radius: 4px; background: #f2f3f5; color: #666e78; font-size: 12px; }.major-job-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }.major-job-summary div { display: grid; justify-items: center; gap: 8px; padding: 28px; border-radius: 12px; background: #fff8f3; }.major-job-summary strong { color: #ff6417; font-size: 27px; }.major-job-summary span { color: #9298a1; }.major-detail-state { padding: 80px 24px; color: #858c95; text-align: center; }.major-detail-state button { border: 0; background: transparent; color: #f06b18; cursor: pointer; }
@media (max-width: 760px) { .major-detail-shell { width: calc(100% - 24px); }.major-detail-head { padding: 22px; }.major-detail-head > div { align-items: flex-start; flex-direction: column; gap: 10px; }.major-detail-tabs { min-height: 58px; }.major-detail-tabs button { font-size: 15px; }.major-detail-panel { padding: 18px; }.major-facts,.major-job-summary { grid-template-columns: 1fr 1fr; }.major-school-filters { grid-template-columns: 1fr 1fr; }.major-school-panel > header small { display: none; } }
@media (max-width: 460px) { .major-facts,.major-job-summary { grid-template-columns: 1fr; }.major-school-filters { grid-template-columns: 1fr; } }
</style>
