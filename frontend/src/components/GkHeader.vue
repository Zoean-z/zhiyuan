<script setup>
import { Search } from "@element-plus/icons-vue";
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import BrandLockup from "./BrandLockup.vue";
import XiaoZhiAvatar from "./XiaoZhiAvatar.vue";
import { AUTH_UPDATED_EVENT, clearStoredAuth, readStoredAuth, subjectTypeLabel } from "../utils/recommendation";

const props = defineProps({
  active: { type: String, default: "首页" },
  adminMode: { type: Boolean, default: false }
});

const router = useRouter();
const auth = ref(readStoredAuth());
const keyword = ref("");

const authMeta = computed(() => {
  const user = auth.value?.user || {};
  return [
    user.examProvince,
    subjectTypeLabel(user.subjectType),
    user.score == null || user.score === "" ? "" : `${user.score}分`
  ]
    .filter(Boolean)
    .join(" | ");
});

/**
 * 导航：支持二级下拉。
 * 修复「导航停在查大学，但只有首页能进入一分一段/招生计划/院校排行」的问题：
 * 这些页面以前只能从首页卡片进，现在全部挂在顶部导航的下拉里，任意页面都能直达。
 */
const NAV_ITEMS = [
  { label: "首页", to: { name: "home" } },
  {
    label: "查大学",
    to: { path: "/schools" },
    children: [
      { label: "查大学", to: { path: "/schools" }, desc: "按省份 / 类型筛选院校" },
      { label: "院校排行", to: { path: "/rank" }, desc: "综合实力榜单" },
      { label: "招生计划", to: { path: "/enroll" }, desc: "分省分专业计划数" },
      { label: "一分一段", to: { path: "/segments" }, desc: "分数对应全省位次" }
    ]
  },
  { label: "查专业", to: { path: "/majors" } },
  {
    label: "志愿填报",
    to: { path: "/volunteer" },
    children: [
      { label: "志愿填报", to: { path: "/volunteer" }, desc: "45 个志愿位模拟填报" }
    ]
  },
  {
    label: "智能选大学",
    to: { path: "/choose" },
    children: [
      { label: "智能选大学", to: { path: "/choose" }, desc: "按分数位次匹配专业组" },
      { label: "AI 推荐查询", to: { path: "/recommend" }, desc: "冲稳保三档推荐结果" }
    ]
  },
  { label: "高考资讯", to: { path: "/news" } }
];

/* 子页面（一分一段、招生计划…）会把父级导航一起点亮 */
function isNavActive(item) {
  if (item.label === props.active) return true;
  return (item.children || []).some((child) => child.label === props.active);
}

const ADMIN_NAV_ITEMS = [
  { label: "用户管理", section: "users" },
  { label: "院校管理", section: "universities" },
  { label: "专业管理", section: "majors" },
  { label: "院校录取线", section: "cutoffs" },
  { label: "专业录取线", section: "majorCutoffs" }
];

function goAdminSection(section) {
  router.push({ name: "admin", query: section === "users" ? {} : { section } });
}

function goAgentWithQuestion(question) {
  router.push({ path: "/agent", query: question ? { q: question } : {} });
}

function submitSearch() {
  const question = String(keyword.value || "").trim();
  if (!question) {
    goAgentWithQuestion();
    return;
  }
  goAgentWithQuestion(`帮我查一下「${question}」相关院校`);
}

function logout() {
  clearStoredAuth();
  auth.value = null;
  router.push({ name: "login" });
}

function refreshAuth(event) {
  auth.value = event?.detail ?? readStoredAuth();
}

onMounted(() => {
  auth.value = readStoredAuth();
  window.addEventListener(AUTH_UPDATED_EVENT, refreshAuth);
});

onBeforeUnmount(() => {
  window.removeEventListener(AUTH_UPDATED_EVENT, refreshAuth);
});
</script>

<template>
  <header class="gk-home__topbar">
    <div class="gk-home__container gk-home__topbar-inner">
      <BrandLockup />
      <div v-if="!adminMode" class="gk-home__search">
        <el-icon class="gk-home__search-icon"><Search /></el-icon>
        <input
          v-model="keyword"
          class="gk-home__search-input"
          type="text"
          placeholder="输入大学 / 专业名称，让 AI 帮你分析"
          @keyup.enter="submitSearch"
        />
        <button class="gk-home__search-btn" type="button" @click="submitSearch">搜索</button>
      </div>
      <div v-else class="gk-admin__brand-tag">管理后台</div>
      <div class="gk-home__topbar-user">
        <template v-if="auth?.token">
          <span v-if="authMeta" class="gk-home__meta">{{ authMeta }}</span>
          <span class="gk-home__hello">Hi，{{ auth?.user?.username || "志愿考生" }}</span>
          <button class="gk-home__link-btn" type="button" @click="logout">退出</button>
        </template>
        <template v-else>
          <button class="gk-home__link-btn" type="button" @click="router.push({ name: 'login', query: { redirect: '/' } })">登录</button>
          <button class="gk-home__reg-btn" type="button" @click="router.push({ name: 'login', query: { redirect: '/' } })">免费注册</button>
        </template>
      </div>
    </div>
  </header>

  <nav class="gk-home__nav">
    <div class="gk-home__container gk-home__nav-inner">
      <template v-if="!adminMode">
        <div v-for="item in NAV_ITEMS" :key="item.label" class="gk-home__nav-cell">
          <button
            type="button"
            class="gk-home__nav-item"
            :class="{ 'is-active': isNavActive(item), 'has-child': !!item.children }"
            @click="router.push(item.to)"
          >
            {{ item.label }}
            <i v-if="item.children" class="gk-home__nav-caret" aria-hidden="true"></i>
          </button>
          <ul v-if="item.children" class="gk-home__nav-drop">
            <li
              v-for="child in item.children"
              :key="child.label"
              :class="{ 'is-active': child.label === props.active }"
              @click="router.push(child.to)"
            >
              <b>{{ child.label }}</b>
              <span>{{ child.desc }}</span>
            </li>
          </ul>
        </div>
      </template>
      <template v-else>
        <button
          v-for="item in ADMIN_NAV_ITEMS"
          :key="item.section"
          type="button"
          class="gk-home__nav-item"
          :class="{ 'is-active': item.label === props.active }"
          @click="goAdminSection(item.section)"
        >
          {{ item.label }}
        </button>
      </template>
    </div>
  </nav>

  <aside
    v-if="!adminMode"
    class="gk-fab"
    title="问小智 · AI 报考助手"
    aria-label="问小智 · AI 报考助手"
    @click="goAgentWithQuestion()"
  >
    <span class="gk-fab__label">问小智 · 在线咨询</span>
    <span class="gk-fab__avatar">
      <XiaoZhiAvatar size="lg" />
      <span class="gk-fab__badge">AI</span>
    </span>
  </aside>
</template>
