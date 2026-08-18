<script setup>
import { Edit, Search } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import BrandLockup from "./BrandLockup.vue";
import { clearStoredAuth, electiveSubjectsLabel, readStoredAuth, subjectTypeLabel } from "../utils/recommendation";

const route = useRoute();
const router = useRouter();
const auth = ref(readStoredAuth());
const keyword = ref(String(route.query.q || ""));

const navItems = [
  { label: "首页", path: "/home" },
  { label: "查大学", path: "/schools" },
  { label: "查专业", path: "/majors" },
  { label: "志愿填报", path: "/recommend" },
  { label: "志愿单", path: "/plans" },
  { label: "AI 助手", path: "/agent" },
  { label: "高考资讯", path: "/news" }
];

const userMeta = computed(() => {
  const user = auth.value?.user || {};
  const subjectLabel = [subjectTypeLabel(user.subjectType), electiveSubjectsLabel(user.electiveSubjects)].filter((value) => value && value !== "-").join("+");
  return [user.examProvince, subjectLabel, user.score == null ? "" : `${user.score}分`]
    .filter(Boolean)
    .join(" · ");
});

function submitSearch() {
  const q = keyword.value.trim();
  router.push({ path: "/schools", query: q ? { q } : {} });
}

function isActive(path) {
  return route.path === path || (path !== "/home" && route.path.startsWith(`${path}/`));
}

function goLogin() {
  router.push({ name: "login", query: { redirect: route.fullPath || "/home" } });
}

function goRegister() {
  router.push({ name: "login", query: { mode: "register", redirect: route.fullPath || "/home" } });
}

function editProfile() {
  router.push({ name: "profile-setup", query: { edit: "1", redirect: route.fullPath } });
}

async function logout() {
  const token = auth.value?.token;
  if (token) {
    try {
      await fetch("/api/auth/logout", { method: "POST", headers: { Authorization: `Bearer ${token}` } });
    } catch {
    }
  }
  clearStoredAuth();
  auth.value = null;
  router.push({ name: "home" });
}

onMounted(() => {
  auth.value = readStoredAuth();
});
</script>

<template>
  <header class="gk-header">
    <div class="gk-header__top gk-container">
      <button class="gk-header__brand" type="button" aria-label="返回首页" @click="router.push('/home')">
        <BrandLockup />
      </button>

      <form class="gk-header__search" role="search" @submit.prevent="submitSearch">
        <el-icon><Search /></el-icon>
        <input v-model="keyword" type="search" placeholder="输入大学名称" aria-label="搜索大学" />
        <button type="submit">搜索</button>
      </form>

      <div class="gk-header__account">
        <template v-if="auth?.token">
          <span v-if="userMeta" class="gk-header__meta">{{ userMeta }}</span>
          <button class="gk-header__edit" type="button" title="编辑个人高考信息" aria-label="编辑个人高考信息" @click="editProfile"><el-icon><Edit /></el-icon></button>
          <strong>{{ auth?.user?.username || "志愿考生" }}</strong>
          <button type="button" @click="logout">退出</button>
        </template>
        <template v-else>
          <button type="button" @click="goLogin">登录</button>
          <button class="gk-header__register" type="button" @click="goRegister">免费注册</button>
        </template>
      </div>
    </div>

    <nav class="gk-header__nav" aria-label="公共导航">
      <div class="gk-container gk-header__nav-inner">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          :class="{ 'is-active': isActive(item.path) }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </div>
    </nav>
  </header>
</template>

<style scoped>
.gk-header {
  position: relative;
  z-index: 10;
  background: #fff;
  box-shadow: 0 1px 0 rgba(31, 41, 55, 0.06);
}

.gk-container {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
}

.gk-header__top {
  min-height: 72px;
  display: grid;
  grid-template-columns: max-content minmax(260px, 560px) max-content;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
}

.gk-header__brand {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.gk-header__search {
  height: 42px;
  display: grid;
  grid-template-columns: 36px 1fr 84px;
  align-items: center;
  border: 2px solid #ff7a1a;
  border-radius: 22px;
  overflow: hidden;
  color: #a0a7b2;
  background: #fff;
}

.gk-header__search .el-icon { justify-self: end; font-size: 17px; }
.gk-header__search input { min-width: 0; height: 100%; border: 0; outline: 0; padding: 0 12px; color: #222; font: inherit; }
.gk-header__search input::placeholder { color: #b4bac3; }
.gk-header__search button,
.gk-header__register {
  align-self: stretch;
  border: 0;
  background: #ff7a1a;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.gk-header__account { display: flex; align-items: center; gap: 14px; white-space: nowrap; font-size: 14px; }
.gk-header__account button { padding: 8px 6px; border: 0; background: transparent; color: #e86612; cursor: pointer; }
.gk-header__account .gk-header__edit { width: 30px; height: 30px; display: grid; place-items: center; padding: 0; border: 1px solid #ffd8bc; border-radius: 50%; background: #fff8f2; }
.gk-header__account .gk-header__register { padding: 9px 18px; border-radius: 20px; color: #fff; }
.gk-header__meta { color: #7a828d; font-size: 12px; }

.gk-header__nav { background: #ff7a1a; }
.gk-header__nav-inner { min-height: 50px; display: flex; align-items: stretch; overflow-x: auto; scrollbar-width: none; }
.gk-header__nav-inner::-webkit-scrollbar { display: none; }
.gk-header__nav button {
  position: relative;
  flex: 0 0 auto;
  min-width: 104px;
  padding: 0 18px;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.86);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}
.gk-header__nav button::after { content: ""; position: absolute; left: 26%; right: 26%; bottom: 7px; height: 3px; border-radius: 2px; background: transparent; }
.gk-header__nav button:hover,
.gk-header__nav button.is-active { color: #fff; }
.gk-header__nav button.is-active::after { background: #fff; }

@media (max-width: 960px) {
  .gk-header__top { grid-template-columns: 1fr max-content; gap: 14px; padding: 14px 0; }
  .gk-header__search { grid-column: 1 / -1; grid-row: 2; max-width: none; }
  .gk-header__meta { display: none; }
}

@media (max-width: 640px) {
  .gk-container { width: min(100% - 24px, 1180px); }
  .gk-header__account strong { display: none; }
  .gk-header__nav-inner { width: 100%; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); overflow: visible; }
  .gk-header__nav button { min-width: 0; min-height: 44px; padding: 0 6px; font-size: 13px; }
  .gk-header__nav button::after { bottom: 4px; }
}
</style>
