import { createRouter, createWebHashHistory } from "vue-router";
import { isUserProfileComplete, readStoredAuth } from "../utils/recommendation";

const routes = [
  { path: "/", redirect: "/recommend" },
  {
    path: "/login",
    name: "login",
    component: () => import("../views/LoginView.vue"),
    meta: { guestOnly: true, standalone: true, title: "登录" }
  },
  {
    path: "/profile-setup",
    name: "profile-setup",
    component: () => import("../views/ProfileSetupView.vue"),
    meta: { requiresAuth: true, standalone: true, profileSetup: true, title: "完善报考信息" }
  },
  { path: "/recommend", name: "recommend", component: () => import("../views/RecommendationView.vue"), meta: { requiresAuth: true, keepAlive: true, title: "推荐查询" } },
  { path: "/agent", name: "agent", component: () => import("../views/AgentView.vue"), meta: { requiresAuth: true, title: "AI 对话" } },
  { path: "/history", name: "history", component: () => import("../views/HistoryRecordsView.vue"), meta: { requiresAuth: true, title: "历史记录" } },
  { path: "/plans", name: "plans", component: () => import("../views/PlansView.vue"), meta: { requiresAuth: true, title: "志愿方案" } },
  { path: "/admin", name: "admin", component: () => import("../views/AdminView.vue"), meta: { requiresAuth: true, requiresAdmin: true, title: "用户管理" } },
  { path: "/:pathMatch(.*)*", redirect: "/recommend" }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

router.beforeEach((to) => {
  const storedAuth = readStoredAuth();
  const hasAuth = Boolean(storedAuth?.token);
  const isAdmin = storedAuth?.user?.role === "ADMIN";
  const profileComplete = isUserProfileComplete(storedAuth?.user);

  if (to.meta.requiresAuth && !hasAuth) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAdmin && !isAdmin) {
    return { name: "recommend" };
  }
  if (hasAuth && isAdmin && to.name !== "admin" && !to.meta.guestOnly) {
    return { name: "admin" };
  }
  if (hasAuth && !isAdmin && !profileComplete && !to.meta.profileSetup) {
    const redirect = to.meta.guestOnly ? undefined : to.fullPath;
    return { name: "profile-setup", query: redirect ? { redirect } : {} };
  }
  if (to.meta.profileSetup && (profileComplete || isAdmin)) {
    return { name: isAdmin ? "admin" : "recommend" };
  }
  if (to.meta.guestOnly && hasAuth) {
    return { name: isAdmin ? "admin" : "recommend" };
  }
  return true;
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 智愿AI报考平台` : "智愿AI报考平台";
});

export default router;
