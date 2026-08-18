<script setup>
import { useRouter } from "vue-router";
import GkSchoolLogo from "./GkSchoolLogo.vue";
import { RANK_LIST } from "../utils/exploreData";

const router = useRouter();
const topSchools = RANK_LIST.slice(0, 10);
</script>

<template>
  <aside class="gk-side">
    <div class="gk-side__card gk-side__promo">
      <p class="gk-side__promo-kicker">2026 智能报考季</p>
      <p class="gk-side__promo-title">AI 帮你定位目标院校</p>
      <button class="gk-side__promo-btn" type="button" @click="router.push({ path: '/agent', query: { q: '帮我定位目标院校，生成一份冲稳保志愿方案' } })">免费测一测</button>
    </div>

    <div class="gk-side__card">
      <div class="gk-side__head">
        <h4>推荐高校</h4>
        <span class="gk-side__more" @click="router.push({ path: '/rank' })">更多 &gt;</span>
      </div>
      <ol class="gk-side__rank">
        <li v-for="school in topSchools" :key="school.id">
          <i class="gk-side__rank-no" :class="{ 'is-top': school.rank <= 3 }">{{ String(school.rank).padStart(2, "0") }}</i>
          <GkSchoolLogo :school="school" size="mini" />
          <span class="gk-side__rank-name">{{ school.name }}</span>
          <span class="gk-side__rank-tag" v-if="school.rank <= 3">历年分数</span>
        </li>
      </ol>
    </div>
  </aside>
</template>
