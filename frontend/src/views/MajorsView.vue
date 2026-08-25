<script setup>
import { Search } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";

const router = useRouter();
const majors = ref([]);
const category = ref("");
const keyword = ref("");
const loading = ref(true);

/* 门类树：由真实专业数据动态生成（major.category 学科门类） */
const categories = computed(() => {
  const names = Array.from(new Set(majors.value.map((m) => m.category).filter(Boolean)));
  return names.map((name) => ({ name, count: majors.value.filter((m) => m.category === name).length }));
});

const filtered = computed(() => {
  const kw = keyword.value.trim();
  return majors.value.filter((major) => {
    if (category.value && major.category !== category.value) return false;
    if (kw && !major.name.includes(kw)) return false;
    return true;
  });
});

function pickCategory(name) {
  category.value = category.value === name ? "" : name;
}

function askMajor(major) {
  router.push({ path: "/agent", query: { q: `${major.name}专业怎么样？就业前景和学习内容介绍一下` } });
}

function openMajor(major, tab = "intro") {
  router.push({ path: `/majors/${major.id}`, query: tab === "schools" ? { tab: "schools" } : {} });
}

onMounted(async () => {
  try {
    const res = await fetch("/api/majors");
    const data = await res.json();
    majors.value = (data.majors || []).map((m) => ({ ...m, category: m.category || "其他" }));
  } catch (e) {
    console.error("加载专业目录失败", e);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="gk-page">
    <GkHeader active="查专业" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gk-filter">
            <div class="gk-filter__row gk-filter__row--search">
              <span class="gk-filter__label">专业</span>
              <div class="gk-filter__search">
                <input v-model="keyword" type="text" placeholder="输入专业名称" @keyup.enter="keyword = keyword.trim()" />
                <button type="button" @click="keyword = keyword.trim()">
                  <el-icon><Search /></el-icon> 搜索
                </button>
              </div>
            </div>
          </div>

          <div class="gk-majors">
            <aside class="gk-majors__tree">
              <p class="gk-majors__tree-title">
                学科门类
                <button
                  v-if="category"
                  type="button"
                  class="gk-majors__tree-clear"
                  @click="category = ''"
                >
                  清空
                </button>
              </p>
              <div v-for="cat in categories" :key="cat.name" class="gk-majors__cat">
                <button
                  type="button"
                  class="gk-majors__cat-name"
                  :class="{ 'is-active': category === cat.name }"
                  @click="pickCategory(cat.name)"
                >
                  {{ cat.name }}
                  <i>{{ cat.count }}</i>
                </button>
              </div>
            </aside>

            <div class="gk-majors__list">
              <p class="gk-page__meta">
                <template v-if="!keyword && !category">热门专业目录</template>
                <template v-else>共 <b>{{ filtered.length }}</b> 个专业</template>
              </p>
              <ul class="gk-major-list">
                <li v-for="major in filtered" :key="major.id" class="gk-major" @click="openMajor(major)">
                  <div class="gk-major__info">
                    <p class="gk-major__name">
                      <button type="button" class="gk-major__link" @click.stop="openMajor(major)">{{ major.name }}</button>
                      <span class="gk-major__code">{{ major.category }}</span>
                    </p>
                    <p class="gk-major__meta">
                      <span>授予学位：{{ major.degreeType || "—" }}</span>
                      <span>选科要求：{{ major.subjectRequirement || "不限" }}</span>
                      <span v-if="major.openSchoolCount">开设院校：{{ major.openSchoolCount }} 所</span>
                    </p>
                    <p class="gk-major__path">{{ major.description || major.category + "门类专业" }} </p>
                  </div>
                  <div class="gk-major__actions">
                    <button type="button" class="gk-major__ghost" @click.stop="askMajor(major)">问前景</button>
                    <button type="button" class="gk-major__action" @click.stop="openMajor(major, 'schools')">开设院校 &gt;</button>
                  </div>
                </li>
                <li v-if="!loading && !filtered.length" class="gk-school__empty">没有符合条件的专业，试试更换门类或关键词</li>
                <li v-if="loading" class="gk-school__empty">正在加载专业目录…</li>
              </ul>
            </div>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
