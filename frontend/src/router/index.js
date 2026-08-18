import { createRouter, createWebHashHistory } from "vue-router";
import { isUserProfileComplete, readStoredAuth } from "../utils/recommendation";

const routes = [
  { path: "/", redirect: "/home" },
  { path: "/home", name: "home", component: () => import("../views/HomeView.vue"), meta: { standalone: true, title: "首页" } },
  { path: "/schools", name: "schools", component: () => import("../views/SchoolsView.vue"), meta: { standalone: true, title: "查大学" } },
  { path: "/majors", name: "majors", component: () => import("../views/MajorsView.vue"), meta: { standalone: true, title: "查专业" } },
  { path: "/majors/:code", name: "major-detail", component: () => import("../views/MajorDetailView.vue"), meta: { standalone: true, title: "专业详情" } },
  { path: "/news", name: "news", component: () => import("../views/NewsView.vue"), meta: { standalone: true, title: "高考资讯" } },
  { path: "/news/:id", name: "news-detail", component: () => import("../views/NewsDetailView.vue"), meta: { standalone: true, title: "资讯详情" } },
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
  { path: "/plans", name: "plans", component: () => import("../views/PlansView.vue"), meta: { requiresAuth: true, title: "志愿方案" } },
  { path: "/admin", name: "admin", component: () => import("../views/AdminView.vue"), meta: { requiresAuth: true, requiresAdmin: true, title: "用户管理" } },
  { path: "/:pathMatch(.*)*", redirect: "/home" }
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
  if (hasAuth && !isAdmin && !profileComplete && to.meta.requiresAuth && !to.meta.profileSetup) {
    const redirect = to.meta.guestOnly ? undefined : to.fullPath;
    return { name: "profile-setup", query: redirect ? { redirect } : {} };
  }
  const isProfileEdit = to.meta.profileSetup && to.query.edit === "1";
  if (to.meta.profileSetup && (isAdmin || (profileComplete && !isProfileEdit))) {
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
