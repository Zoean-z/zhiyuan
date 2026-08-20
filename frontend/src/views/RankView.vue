<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { RANK_LIST, SCHOOL_TYPES, schoolLoc, schoolTags } from "../utils/exploreData";

const router = useRouter();
const rankType = ref("综合榜");
const typeFilter = ref("全部");
const page = ref(1);
const PAGE_SIZE = 10;

const RANK_TYPES = ["综合榜", "理工榜", "综合类榜"];

const rows = computed(() => {
  let list = RANK_LIST;
  if (rankType.value === "理工榜") list = list.filter((s) => s.type === "理工类");
  if (rankType.value === "综合类榜") list = list.filter((s) => s.type === "综合类");
  if (typeFilter.value !== "全部") list = list.filter((s) => s.type === typeFilter.value);
  return list.map((s, i) => ({ ...s, rank: i + 1 }));
});

const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / PAGE_SIZE)));
const pageRows = computed(() => {
  const safePage = Math.min(page.value, totalPages.value);
  return rows.value.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);
});

function switchRankType(t) {
  rankType.value = t;
  page.value = 1;
}

/* 【修复】原来点「测录取」是拉起 AI 对话问一句，看不到院校详情；
   现在统一进 /schools/:id 院校详情页（里面有近三年录取 + 概率拆解） */
function askAdmission(school) {
  router.push({ name: "school-detail", params: { id: school.id } });
}
</script>

<template>
  <div class="gk-page">
    <GkHeader active="院校排行" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <div class="gk-filter">
            <div class="gk-filter__row">
              <span class="gk-filter__label">排名类型</span>
              <button
                v-for="t in RANK_TYPES"
                :key="t"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': rankType === t }"
                @click="switchRankType(t)"
              >
                {{ t }}
              </button>
            </div>
            <div class="gk-filter__row">
              <span class="gk-filter__label">类型</span>
              <button
                v-for="t in ['全部', ...SCHOOL_TYPES.slice(1)]"
                :key="t"
                type="button"
                class="gk-filter__opt"
                :class="{ 'is-active': typeFilter === t }"
                @click="typeFilter = t; page = 1"
              >
                {{ t }}
              </button>
            </div>
          </div>

          <ul class="gk-rank-list">
            <li v-for="school in pageRows" :key="school.id" class="gk-rank">
              <span class="gk-rank__no" :class="{ 'is-top': school.rank <= 3 }">{{ school.rank }}</span>
              <GkSchoolLogo :school="school" size="sm" />
              <div class="gk-rank__info">
                <p class="gk-school__name">
                  {{ school.name }}
                  <span class="gk-school__loc">@{{ schoolLoc(school) }}</span>
                </p>
                <p class="gk-school__badges">
                  <i v-for="tag in schoolTags(school)" :key="tag">{{ tag }}</i>
                </p>
              </div>
              <span class="gk-rank__index">{{ school.index }}</span>
              <div class="gk-rank__actions">
                <button class="gk-rank__btn gk-rank__btn--primary" type="button" @click="askAdmission(school)">测录取</button>
                <button class="gk-rank__btn" type="button" @click="router.push({ path: '/agent', query: { q: `对比分析${school.name}和同层次院校的优劣` } })">PK对比</button>
              </div>
            </li>
          </ul>

          <div class="gk-pager">
            <button type="button" :disabled="page <= 1" @click="page -= 1">‹ 上一页</button>
            <button
              v-for="p in totalPages"
              :key="p"
              type="button"
              :class="{ 'is-active': p === page }"
              @click="page = p"
            >
              {{ p }}
            </button>
            <button type="button" :disabled="page >= totalPages" @click="page += 1">下一页 ›</button>
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
