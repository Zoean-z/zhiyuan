<script setup>
import { Search, TrendCharts } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { profile } from "../utils/examProfile";

const router = useRouter();
const schools = ref([]);
const loading = ref(false);
const provinceFilter = ref("全部");
const levelFilter = ref("all");
const sortKey = ref("default");
const keyword = ref("");

const SCHOOL_LEVELS = [
  { key: "all", label: "全部" },
  { key: "985", label: "985" },
  { key: "211", label: "211" },
  { key: "isDoubleFirstClass", label: "双一流" }
];

const SCHOOL_PROVINCES = computed(() => ["全部", ...new Set(schools.value.map((s) => s.province).filter(Boolean))]);

const SORTS = [
  { label: "默认排序", key: "default" },
  { label: "招生人数由高到低", key: "planCount" },
  { label: "招生专业由多到少", key: "majorCount" }
];

/* 数据源：后端 /api/universities（1137 所真实大学 + 按考生省份聚合的计划数/专业数） */
async function fetchSchools() {
  if (loading.value) return;
  loading.value = true;
  try {
    const examProvince = profile.province || "";
    const base = `/api/universities?size=100&examProvince=${encodeURIComponent(examProvince)}`;
    const first = await (await fetch(base + "&page=1")).json();
    const total = Number(first.total || 0);
    const pages = Math.max(1, Math.ceil(total / 100));
    const all = [...(first.items || [])];
    for (let p = 2; p <= Math.min(pages, 15); p++) {
      const pageData = await (await fetch(base + `&page=${p}`)).json();
      all.push(...(pageData.items || []));
    }
    schools.value = all;
  } catch (ex) {
    console.error("加载院校失败", ex);
  } finally {
    loading.value = false;
  }
}

onMounted(fetchSchools);

// 计划趋势示意：基于计划数生成稳定 4 年序列（非数据库字段）
function trendBars(school) {
  const base = school.planCount || 100;
  return [0, 1, 2, 3].map((i) => Math.max(20, base - 12 * (3 - i)));
}

const filtered = computed(() => {
  const kw = keyword.value.trim();
  const list = schools.value.filter((school) => {
    if (provinceFilter.value !== "全部" && school.province !== provinceFilter.value) return false;
    if (levelFilter.value === "985" && !school.is985) return false;
    if (levelFilter.value === "211" && !school.is211) return false;
    // 双一流 ≡ 211：选双一流时 985/211 院校同样命中（20260820 概念归并）
    if (levelFilter.value === "isDoubleFirstClass" && !school.is985 && !school.is211) return false;
    if (kw && !school.name.includes(kw)) return false;
    return true;
  });
  if (sortKey.value === "planCount") return [...list].sort((a, b) => (b.planCount ?? 0) - (a.planCount ?? 0));
  if (sortKey.value === "majorCount") return [...list].sort((a, b) => (b.majorCount ?? 0) - (a.majorCount ?? 0));
  return list;
});

function schoolTags(school) {
  const tags = [school.schoolType || "本科"];
  if (school.nature) tags.push(school.nature);
  if (school.is985) tags.push("985");
  if (school.is211 && !school.is985) tags.push("211");
  return tags;
}

function askPlan(school) {
  router.push({ path: "/agent", query: { q: `查询${school.name}2026年招生计划与专业` } });
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="招生计划" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gk-filter">
            <div class="gk-filter__row">
              <span class="gk-filter__label">年份</span>
              <button type="button" class="gk-filter__opt is-active">2026</button>
              <button type="button" class="gk-filter__opt">2025</button>
              <button type="button" class="gk-filter__opt">2024</button>
              <span class="gk-filter__label gk-filter__label--gap">位置</span>
              <button
                v-for="p in SCHOOL_PROVINCES"
                :key="p"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': provinceFilter === p }"
                @click="provinceFilter = p"
              >
                {{ p }}
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">层次</span>
              <button
                v-for="l in SCHOOL_LEVELS"
                :key="l.key"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': levelFilter === l.key }"
                @click="levelFilter = l.key"
              >
                {{ l.label }}
              </button>
              <span class="gk-filter__label gk-filter__label--gap">排序</span>
              <button
                v-for="s in SORTS"
                :key="s.key"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': sortKey === s.key }"
                @click="sortKey = s.key"
              >
                {{ s.label }}
              </button>
            </div>
            <div class="gk-filter__row gk-filter__row--search">
              <span class="gk-filter__label">校名</span>
              <div class="gk-filter__search">
                <input v-model="keyword" type="text" placeholder="输入院校名称" @keyup.enter="keyword = keyword.trim()" />
                <button type="button" @click="keyword = keyword.trim()">
                  <el-icon><Search /></el-icon> 搜索
                </button>
              </div>
            </div>
          </div>

          <p class="gk-page__meta">共 <b>{{ filtered.length }}</b> 所院校招生计划</p>

          <ul class="gk-enroll-list">
            <li v-for="school in filtered" :key="school.id" class="gk-enroll">
              <GkSchoolLogo :school="school" />
              <div class="gk-enroll__info">
                <p class="gk-school__name">
                  {{ school.name }}
                  <span class="gk-school__loc">@{{ school.province }}</span>
                </p>
                <p class="gk-school__badges">
                  <i v-for="tag in schoolTags(school)" :key="tag">{{ tag }}</i>
                </p>
              </div>
              <div class="gk-enroll__stats">
                <div class="gk-enroll__stat">
                  <b>{{ school.planCount ?? "—" }}</b>
                  <span>26招生计划/人</span>
                </div>
                <div class="gk-enroll__stat">
                  <b>{{ school.majorCount ?? "—" }}</b>
                  <span>招生专业/个</span>
                </div>
                <div class="gk-enroll__stat">
                  <b>{{ school.cutoffScore ?? "—" }}</b>
                  <span>参考最低分</span>
                </div>
                <div class="gk-enroll__trend" title="计划趋势">
                  <el-icon><TrendCharts /></el-icon>
                  <div class="gk-enroll__bars">
                    <i
                      v-for="(bar, bi) in trendBars(school)"
                      :key="bi"
                      :style="{ height: `${Math.min(100, (bar / school.planCount) * 100)}%` }"
                      :class="{ 'is-last': bi === 3 }"
                    ></i>
                  </div>
                </div>
              </div>
              <button class="gk-school__action" type="button" @click="askPlan(school)">查看计划 &gt;</button>
            </li>
            <li v-if="!filtered.length" class="gk-school__empty">没有符合条件的院校，试试放宽筛选条件</li>
          </ul>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
