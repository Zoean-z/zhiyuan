<script setup>
import { Search } from "@element-plus/icons-vue";
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { MAJOR_CATEGORIES, MAJOR_LEVELS, MAJORS } from "../utils/exploreData";

const router = useRouter();
const level = ref(0);
const category = ref("");
const sub = ref("");
const keyword = ref("");
const expanded = ref("");

const filtered = computed(() => {
  const kw = keyword.value.trim();
  return MAJORS.filter((major) => {
    if (major.level !== level.value) return false;
    if (category.value && major.category !== category.value) return false;
    if (sub.value && major.sub !== sub.value) return false;
    if (kw && !major.name.includes(kw) && !major.code.includes(kw)) return false;
    return true;
  });
});

function pickCategory(name) {
  if (category.value === name && !sub.value) {
    category.value = "";
  } else {
    category.value = name;
    sub.value = "";
  }
  expanded.value = expanded.value === name ? "" : name;
}

function pickSub(name, parent) {
  category.value = parent;
  sub.value = sub.value === name ? "" : name;
}

function switchLevel(index) {
  level.value = index;
  category.value = "";
  sub.value = "";
}

function askMajor(major) {
  router.push({ path: "/agent", query: { q: `${major.name}专业怎么样？就业前景和学习内容介绍一下` } });
}

function openMajor(major, tab = "intro") {
  router.push({ path: `/majors/${major.code}`, query: tab === "schools" ? { tab: "schools" } : {} });
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="查专业" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gk-filter">
            <div class="gk-filter__row">
              <span class="gk-filter__label">层次</span>
              <button
                v-for="(l, i) in MAJOR_LEVELS"
                :key="l"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': level === i }"
                @click="switchLevel(i)"
              >
                {{ l }}
              </button>
            </div>
            <div class="gk-filter__row gk-filter__row--search">
              <span class="gk-filter__label">专业</span>
              <div class="gk-filter__search">
                <input v-model="keyword" type="text" placeholder="输入专业名称或专业代码" @keyup.enter="keyword = keyword.trim()" />
                <button type="button" @click="keyword = keyword.trim()">
                  <el-icon><Search /></el-icon> 搜索
                </button>
              </div>
            </div>
          </div>

          <div class="gk-majors">
            <aside class="gk-majors__tree">
              <p class="gk-majors__tree-title">
                热门门类
                <button
                  v-if="category || sub"
                  type="button"
                  class="gk-majors__tree-clear"
                  @click="category = ''; sub = ''"
                >
                  清空
                </button>
              </p>
              <div v-for="cat in MAJOR_CATEGORIES" :key="cat.name" class="gk-majors__cat">
                <button
                  type="button"
                  class="gk-majors__cat-name"
                  :class="{ 'is-active': category === cat.name }"
                  @click="pickCategory(cat.name)"
                >
                  {{ cat.name }}
                  <i :class="{ 'is-open': expanded === cat.name }">›</i>
                </button>
                <div v-if="expanded === cat.name" class="gk-majors__subs">
                  <button
                    v-for="s in cat.subs"
                    :key="s"
                    type="button"
                    class="gk-majors__sub"
                    :class="{ 'is-active': sub === s }"
                    @click="pickSub(s, cat.name)"
                  >
                    {{ s }}
                  </button>
                </div>
              </div>
            </aside>

            <div class="gk-majors__list">
              <p class="gk-page__meta">
                <template v-if="!keyword && !category && level === 0">用户搜索专业 TOP30</template>
                <template v-else>共 <b>{{ filtered.length }}</b> 个专业</template>
              </p>
              <ul class="gk-major-list">
                <li v-for="major in filtered" :key="major.code" class="gk-major" @click="openMajor(major)">
                  <div class="gk-major__info">
                    <p class="gk-major__name">
                      <button type="button" class="gk-major__link" @click.stop="openMajor(major)">{{ major.name }}</button>
                      <span class="gk-major__code">{{ major.code }}</span>
                    </p>
                    <p class="gk-major__meta">
                      <span>修业年限：{{ major.duration }}</span>
                      <span>授予学位：{{ major.degree }}</span>
                      <span>男女比例：{{ major.gender }}</span>
                      <span>平均年薪：{{ major.salary }}</span>
                    </p>
                    <p class="gk-major__path">{{ major.category }} · {{ major.sub }} · {{ major.schoolCount }} 所院校开设</p>
                  </div>
                  <div class="gk-major__actions">
                    <button type="button" class="gk-major__ghost" @click.stop="askMajor(major)">问前景</button>
                    <button type="button" class="gk-major__action" @click.stop="openMajor(major, 'schools')">开设院校 &gt;</button>
                  </div>
                </li>
                <li v-if="!filtered.length" class="gk-school__empty">没有符合条件的专业，试试更换门类或关键词</li>
              </ul>
            </div>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
