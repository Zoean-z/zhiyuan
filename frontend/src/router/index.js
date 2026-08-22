import { createRouter, createWebHashHistory } from "vue-router";
import { isUserProfileComplete, readStoredAuth } from "../utils/recommendation";

const routes = [
  { path: "/", name: "home", component: () => import("../views/HomeView.vue"), meta: { title: "首页" } },
  { path: "/schools", name: "schools", component: () => import("../views/SchoolsView.vue"), meta: { title: "查大学" } },
  { path: "/schools/:id", name: "school-detail", component: () => import("../views/SchoolDetailView.vue"), meta: { title: "院校详情" } },
  { path: "/majors", name: "majors", component: () => import("../views/MajorsView.vue"), meta: { title: "查专业" } },
  { path: "/majors/:code", name: "major-detail", component: () => import("../views/MajorDetailView.vue"), meta: { title: "专业详情" } },
  { path: "/volunteer", name: "volunteer", component: () => import("../views/VolunteerView.vue"), meta: { title: "志愿填报" } },
  { path: "/choose", name: "choose", component: () => import("../views/ChooseView.vue"), meta: { title: "智能选大学" } },
  { path: "/rank", name: "rank", component: () => import("../views/RankView.vue"), meta: { title: "院校排行" } },
  { path: "/segments", name: "segments", component: () => import("../views/SegmentsView.vue"), meta: { title: "一分一段" } },
  { path: "/enroll", name: "enroll", component: () => import("../views/EnrollPlanView.vue"), meta: { title: "招生计划" } },
  { path: "/news", name: "news", component: () => import("../views/NewsView.vue"), meta: { title: "高考资讯" } },
  { path: "/news/:id", name: "news-detail", component: () => import("../views/NewsDetailView.vue"), meta: { title: "资讯详情" } },
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
  { path: "/recommend", name: "recommend", component: () => import("../views/RecommendationView.vue"), meta: { requiresAuth: true, standalone: true, title: "推荐查询" } },
  { path: "/agent", name: "agent", component: () => import("../views/AgentView.vue"), meta: { requiresAuth: true, standalone: true, title: "AI 对话" } },
  { path: "/history", name: "history", component: () => import("../views/HistoryRecordsView.vue"), meta: { requiresAuth: true, standalone: true, title: "历史记录" } },
  { path: "/plans", name: "plans", component: () => import("../views/PlansView.vue"), meta: { requiresAuth: true, standalone: true, title: "志愿方案" } },
  { path: "/admin", name: "admin", component: () => import("../views/AdminView.vue"), meta: { requiresAuth: true, requiresAdmin: true, standalone: true, title: "管理后台" } },
  { path: "/:pathMatch(.*)*", redirect: "/" }
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
  // Admins are only redirected to the console after login (guestOnly routes below).
  // They can still browse every public and authenticated page like any other user.
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
