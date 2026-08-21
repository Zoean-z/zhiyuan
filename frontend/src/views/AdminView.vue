<script setup>
import {
  CircleCloseFilled,
  Collection,
  DataAnalysis,
  EditPen,
  Lock,
  Plus,
  Reading,
  Refresh,
  School,
  Search,
  SwitchButton,
  TrendCharts,
  User,
  UserFilled
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { computed, inject, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import BrandLockup from "../components/BrandLockup.vue";
import sidebarArt from "../assets/admission-journey.png";
import { clearStoredAuth, readStoredAuth } from "../utils/recommendation";

defineOptions({ name: "AdminView" });

const route = useRoute();
const router = useRouter();
const workspace = inject("workspace", null);

const SECTION_META = {
  dashboard: { title: "管理工作台", subtitle: "查看平台运行与数据准备情况", icon: DataAnalysis },
  users: { title: "用户管理", subtitle: "维护账号角色、状态与报考资料", icon: User },
  universities: { title: "院校管理", subtitle: "维护推荐与查询使用的院校主数据", icon: School },
  majors: { title: "专业管理", subtitle: "维护专业分类、选科要求与说明", icon: Reading },
  cutoffs: { title: "院校录取线", subtitle: "维护分省、分科类、分年份院校录取数据", icon: TrendCharts },
  majorCutoffs: { title: "专业录取线", subtitle: "维护院校专业维度的录取事实数据", icon: Collection }
};

const NAV_GROUPS = [
  { label: "业务总览", items: [{ section: "dashboard", ...SECTION_META.dashboard }] },
  {
    label: "账号与内容",
    items: [
      { section: "users", ...SECTION_META.users },
      { section: "universities", ...SECTION_META.universities },
      { section: "majors", ...SECTION_META.majors }
    ]
  },
  {
    label: "招生数据",
    items: [
      { section: "cutoffs", ...SECTION_META.cutoffs },
      { section: "majorCutoffs", ...SECTION_META.majorCutoffs }
    ]
  }
];

const rawSection = computed(() => String(route.query.section || "dashboard"));
const section = computed(() => (SECTION_META[rawSection.value] ? rawSection.value : "dashboard"));
const sectionMeta = computed(() => SECTION_META[section.value]);
const storedAuth = computed(() => workspace?.auth?.value || readStoredAuth());
const token = computed(() => storedAuth.value?.token || "");
const currentUsername = computed(() => storedAuth.value?.user?.username || "管理员");
const currentUserInitial = computed(() => currentUsername.value.slice(0, 1).toUpperCase());

const loading = ref(false);
const lastSyncedAt = ref("");
const pageSize = 10;
const overview = reactive({ totalCount: 0, userCount: 0, adminCount: 0, disabledCount: 0 });
const platformSnapshot = reactive({ latestYear: null, provinceCount: 0 });
const dashboardUsers = ref([]);

const users = ref([]);
const userFilters = reactive({ keyword: "", role: "", enabled: "" });
const userPage = ref(1);
const pagedUsers = computed(() => users.value.slice((userPage.value - 1) * pageSize, userPage.value * pageSize));

const universities = ref([]);
const universityKeyword = ref("");
const universityTier = ref("");
const universityPage = ref(1);
const universityTiers = computed(() => [...new Set(universities.value.map((item) => item.tier).filter(Boolean))]);
const filteredUniversities = computed(() => {
  const keyword = universityKeyword.value.trim().toLowerCase();
  return universities.value.filter((item) => {
    const searchText = `${item.name || ""}${item.province || ""}${item.tier || ""}`.toLowerCase();
    return (!keyword || searchText.includes(keyword)) && (!universityTier.value || item.tier === universityTier.value);
  });
});
const pagedUniversities = computed(() => filteredUniversities.value.slice((universityPage.value - 1) * pageSize, universityPage.value * pageSize));

const majors = ref([]);
const majorKeyword = ref("");
const majorCategory = ref("");
const majorDegreeType = ref("");
const majorPage = ref(1);
const majorCategories = computed(() => [...new Set(majors.value.map((item) => item.category).filter(Boolean))]);
const majorDegreeTypes = computed(() => [...new Set(majors.value.map((item) => item.degreeType).filter(Boolean))]);
const filteredMajors = computed(() => {
  const keyword = majorKeyword.value.trim().toLowerCase();
  return majors.value.filter((item) => {
    const searchText = `${item.name || ""}${item.category || ""}`.toLowerCase();
    return (!keyword || searchText.includes(keyword))
      && (!majorCategory.value || item.category === majorCategory.value)
      && (!majorDegreeType.value || item.degreeType === majorDegreeType.value);
  });
});
const pagedMajors = computed(() => filteredMajors.value.slice((majorPage.value - 1) * pageSize, majorPage.value * pageSize));

const cutoffs = ref([]);
const cutoffFilters = reactive({ universityId: "", admissionYear: "", province: "", subjectType: "" });
const cutoffPage = ref(1);
const pagedCutoffs = computed(() => cutoffs.value.slice((cutoffPage.value - 1) * pageSize, cutoffPage.value * pageSize));

const majorCutoffs = ref([]);
const majorCutoffFilters = reactive({ universityId: "", admissionYear: "", province: "", subjectType: "", majorKeyword: "" });
const majorCutoffPage = ref(1);
const pagedMajorCutoffs = computed(() => majorCutoffs.value.slice((majorCutoffPage.value - 1) * pageSize, majorCutoffPage.value * pageSize));

const businessTotals = computed(() => dashboardUsers.value.reduce((totals, user) => ({
  recommendations: totals.recommendations + Number(user.recommendationCount || 0),
  plans: totals.plans + Number(user.planCount || 0),
  conversations: totals.conversations + Number(user.conversationCount || 0)
}), { recommendations: 0, plans: 0, conversations: 0 }));

const dataHealth = computed(() => {
  const universityIds = new Set(universities.value.map((item) => Number(item.id)).filter(Number.isFinite));
  const universitiesWithCutoff = new Set(
    [...cutoffs.value, ...majorCutoffs.value]
      .map((item) => Number(item.universityId))
      .filter((id) => universityIds.has(id))
  );
  const majorIds = new Set(majors.value.map((item) => Number(item.id)).filter(Number.isFinite));
  const coveredMajorIds = new Set(
    majorCutoffs.value.map((item) => Number(item.majorId)).filter((id) => majorIds.has(id))
  );
  const coveredMajorNames = new Set(majorCutoffs.value.map((item) => String(item.majorName || "").trim()).filter(Boolean));
  const coveredMajors = majors.value.filter((item) => coveredMajorIds.has(Number(item.id)) || coveredMajorNames.has(String(item.name || "").trim())).length;
  const candidateUsers = dashboardUsers.value.filter((user) => user.role !== "ADMIN");
  const completedProfiles = candidateUsers.filter((user) => user.examProvince && user.subjectType && user.score != null).length;
  const allYears = [...cutoffs.value, ...majorCutoffs.value]
    .map((item) => Number(item.admissionYear))
    .filter(Number.isFinite);
  const provinces = new Set([...cutoffs.value, ...majorCutoffs.value].map((item) => item.province).filter(Boolean));
  const percent = (value, total) => (total ? Math.round((value / total) * 100) : 0);
  return {
    universityCoverage: percent(universitiesWithCutoff.size, universities.value.length),
    majorCoverage: percent(coveredMajors, majors.value.length),
    profileCompletion: percent(completedProfiles, candidateUsers.length),
    universitiesWithoutCutoff: Math.max(0, universities.value.length - universitiesWithCutoff.size),
    majorsWithoutCutoff: Math.max(0, majors.value.length - coveredMajors),
    incompleteProfiles: Math.max(0, candidateUsers.length - completedProfiles),
    candidateCount: candidateUsers.length,
    latestYear: allYears.length ? Math.max(...allYears) : null,
    provinceCount: provinces.size
  };
});

const recentCutoffs = computed(() => [...cutoffs.value]
  .sort((a, b) => Number(b.admissionYear || 0) - Number(a.admissionYear || 0) || Number(b.id || 0) - Number(a.id || 0))
  .slice(0, 5));

const attentionItems = computed(() => {
  const universityItem = universities.value.length
    ? {
        label: "院校录取数据覆盖",
        detail: dataHealth.value.universitiesWithoutCutoff
          ? `${dataHealth.value.universitiesWithoutCutoff} 所院校尚无院校/专业录取数据`
          : "当前院校均已关联录取数据",
        value: `${dataHealth.value.universityCoverage}%`,
        section: "cutoffs",
        tone: dataHealth.value.universitiesWithoutCutoff ? "warning" : "success"
      }
    : {
        label: "院校主数据",
        detail: "暂无院校主数据，需先建立推荐与录取线关联基础",
        value: "待录入",
        section: "universities",
        tone: "info"
      };
  const majorItem = majors.value.length
    ? {
        label: "专业录取数据覆盖",
        detail: dataHealth.value.majorsWithoutCutoff
          ? `${dataHealth.value.majorsWithoutCutoff} 个专业尚无专业录取数据`
          : "当前专业均已关联录取数据",
        value: `${dataHealth.value.majorCoverage}%`,
        section: "majorCutoffs",
        tone: dataHealth.value.majorsWithoutCutoff ? "warning" : "success"
      }
    : {
        label: "专业主数据",
        detail: "暂无专业主数据，需先建立专业与录取线关联基础",
        value: "待录入",
        section: "majors",
        tone: "info"
      };
  const profileItem = dataHealth.value.candidateCount
    ? {
        label: "考生档案完整度",
        detail: dataHealth.value.incompleteProfiles
          ? `${dataHealth.value.incompleteProfiles} 个普通用户尚未完善省份、科类或分数`
          : "所有普通用户均已完善报考资料",
        value: `${dataHealth.value.profileCompletion}%`,
        section: "users",
        tone: dataHealth.value.incompleteProfiles ? "info" : "success"
      }
    : {
        label: "考生档案完整度",
        detail: "暂无普通用户，当前没有可计算的考生档案",
        value: "待产生",
        section: "users",
        tone: "info"
      };
  return [
    universityItem,
    majorItem,
    profileItem,
    {
      label: "停用账号",
      detail: overview.disabledCount ? `${overview.disabledCount} 个账号当前不可登录` : "暂无停用账号",
      value: String(overview.disabledCount || 0),
      section: "users",
      tone: overview.disabledCount ? "danger" : "success"
    }
  ];
});

const settingsVisible = ref(false);
const settingsSubmitting = ref(false);
const selectedUser = ref(null);
const settingsForm = reactive({ role: "USER", enabled: true });

const recordDialogVisible = ref(false);
const recordSubmitting = ref(false);
const editingId = ref(null);
const recordForm = reactive({
  name: "", province: "", tier: "", is985: false, is211: false, isDoubleFirstClass: false, tags: "",
  category: "", degreeType: "", subjectRequirement: "", description: "",
  universityId: "", majorId: "", majorName: "", admissionYear: new Date().getFullYear(), subjectType: "PHYSICS", cutoffScore: "", minRank: ""
});

const recordDialogTitle = computed(() => {
  const labels = { universities: "院校", majors: "专业", cutoffs: "院校录取线", majorCutoffs: "专业录取线" };
  return `${editingId.value ? "编辑" : "新增"}${labels[section.value] || "数据"}`;
});

function goSection(nextSection) {
  router.push({ name: "admin", query: nextSection === "dashboard" ? {} : { section: nextSection } });
}

async function logout() {
  if (typeof workspace?.logout === "function") {
    await workspace.logout();
    return;
  }
  clearStoredAuth();
  if (workspace?.auth) workspace.auth.value = null;
  await router.replace({ name: "login" });
}

async function api(url, options = {}) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(url, {
      ...options,
      signal: options.signal || controller.signal,
      headers: {
        Authorization: `Bearer ${token.value}`,
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {})
      }
    });
    const data = response.headers.get("content-type")?.includes("application/json") ? await response.json() : null;
    if (!response.ok) {
      if (response.status === 401) {
        clearStoredAuth();
        if (workspace?.auth) workspace.auth.value = null;
        router.replace({ name: "login" });
      }
      throw new Error(data?.message || data?.error || "操作失败，请稍后重试");
    }
    return data;
  } catch (error) {
    if (error?.name === "AbortError") throw new Error("请求超时，请稍后重试");
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

function formatDate(value) {
  if (!value) return "—";
  return String(value).replace("T", " ").slice(0, 19);
}

function formatNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number.toLocaleString("zh-CN") : "—";
}

function subjectLabel(value) {
  if (value === "PHYSICS" || value === "物理") return "物理类";
  if (value === "HISTORY" || value === "历史") return "历史类";
  return value || "—";
}

function profileLabel(user) {
  if (!user?.examProvince || !user?.subjectType || user?.score == null) return "未完善";
  return `${user.examProvince} · ${subjectLabel(user.subjectType)} · ${user.score} 分`;
}

function universityName(id) {
  return universities.value.find((item) => Number(item.id) === Number(id))?.name || `院校 #${id}`;
}

function updateSyncTime() {
  lastSyncedAt.value = new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  }).format(new Date());
}

async function loadDashboard() {
  const [summary, userList, universityList, majorList, cutoffList, majorCutoffList] = await Promise.all([
    api("/api/admin/users/overview"),
    api("/api/admin/users"),
    api("/api/admin/universities"),
    api("/api/admin/majors"),
    api("/api/admin/admission-cutoffs"),
    api("/api/admin/major-admission-cutoffs")
  ]);
  Object.assign(overview, summary || {});
  dashboardUsers.value = Array.isArray(userList) ? userList : [];
  universities.value = Array.isArray(universityList) ? universityList : [];
  majors.value = Array.isArray(majorList) ? majorList : [];
  cutoffs.value = Array.isArray(cutoffList) ? cutoffList : [];
  majorCutoffs.value = Array.isArray(majorCutoffList) ? majorCutoffList : [];
  platformSnapshot.latestYear = dataHealth.value.latestYear;
  platformSnapshot.provinceCount = dataHealth.value.provinceCount;
}

async function loadUsers() {
  const params = new URLSearchParams();
  if (userFilters.keyword.trim()) params.set("keyword", userFilters.keyword.trim());
  if (userFilters.role) params.set("role", userFilters.role);
  if (userFilters.enabled !== "") params.set("enabled", userFilters.enabled);
  const [list, summary] = await Promise.all([
    api(`/api/admin/users${params.size ? `?${params}` : ""}`),
    api("/api/admin/users/overview")
  ]);
  users.value = Array.isArray(list) ? list : [];
  Object.assign(overview, summary || {});
  userPage.value = 1;
}

async function loadUniversities() {
  const list = await api("/api/admin/universities");
  universities.value = Array.isArray(list) ? list : [];
  universityPage.value = 1;
}

async function loadMajors() {
  const list = await api("/api/admin/majors");
  majors.value = Array.isArray(list) ? list : [];
  majorPage.value = 1;
}

function appendQuery(params, key, value) {
  if (value !== "" && value != null) params.set(key, value);
}

async function ensureUniversities() {
  if (!universities.value.length) await loadUniversities();
}

async function loadCutoffs() {
  await ensureUniversities();
  const params = new URLSearchParams();
  Object.entries(cutoffFilters).forEach(([key, value]) => appendQuery(params, key, value));
  const list = await api(`/api/admin/admission-cutoffs${params.size ? `?${params}` : ""}`);
  cutoffs.value = Array.isArray(list) ? list : [];
  cutoffPage.value = 1;
}

async function loadMajorCutoffs() {
  await ensureUniversities();
  const params = new URLSearchParams();
  Object.entries(majorCutoffFilters).forEach(([key, value]) => appendQuery(params, key, value));
  const list = await api(`/api/admin/major-admission-cutoffs${params.size ? `?${params}` : ""}`);
  majorCutoffs.value = Array.isArray(list) ? list : [];
  majorCutoffPage.value = 1;
}

async function loadSection() {
  loading.value = true;
  try {
    if (section.value === "dashboard") await loadDashboard();
    else if (section.value === "users") await loadUsers();
    else if (section.value === "universities") await loadUniversities();
    else if (section.value === "majors") await loadMajors();
    else if (section.value === "cutoffs") await loadCutoffs();
    else if (section.value === "majorCutoffs") await loadMajorCutoffs();
    updateSyncTime();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}

function resetUserFilters() {
  Object.assign(userFilters, { keyword: "", role: "", enabled: "" });
  loadSection();
}

function resetUniversityFilters() {
  universityKeyword.value = "";
  universityTier.value = "";
  universityPage.value = 1;
}

function resetMajorFilters() {
  majorKeyword.value = "";
  majorCategory.value = "";
  majorDegreeType.value = "";
  majorPage.value = 1;
}

function resetCutoffFilters() {
  Object.assign(cutoffFilters, { universityId: "", admissionYear: "", province: "", subjectType: "" });
  loadSection();
}

function resetMajorCutoffFilters() {
  Object.assign(majorCutoffFilters, { universityId: "", admissionYear: "", province: "", subjectType: "", majorKeyword: "" });
  loadSection();
}

function openUserSettings(user) {
  selectedUser.value = user;
  settingsForm.role = user.role || "USER";
  settingsForm.enabled = user.enabled !== false;
  settingsVisible.value = true;
}

async function saveUserSettings() {
  if (!selectedUser.value) return;
  settingsSubmitting.value = true;
  try {
    await api(`/api/admin/users/${selectedUser.value.id}/settings`, {
      method: "PUT",
      body: JSON.stringify(settingsForm)
    });
    settingsVisible.value = false;
    ElMessage.success("用户设置已保存");
    await loadSection();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    settingsSubmitting.value = false;
  }
}

function resetRecordForm() {
  Object.assign(recordForm, {
    name: "", province: "", tier: "", is985: false, is211: false, isDoubleFirstClass: false, tags: "",
    category: "", degreeType: "", subjectRequirement: "", description: "",
    universityId: "", majorId: "", majorName: "", admissionYear: new Date().getFullYear(), subjectType: "PHYSICS", cutoffScore: "", minRank: ""
  });
}

function openRecordDialog(record = null) {
  resetRecordForm();
  editingId.value = record?.id || null;
  if (record) {
    Object.keys(recordForm).forEach((key) => {
      if (record[key] != null) recordForm[key] = record[key];
    });
    if (section.value === "universities") {
      recordForm.isDoubleFirstClass = record.isDoubleFirstClass === true || record.is211 === true;
    }
  }
  recordDialogVisible.value = true;
}

function nullableNumber(value) {
  return value === "" || value == null ? null : Number(value);
}

function buildRecordRequest() {
  if (section.value === "universities") {
    if (!recordForm.name.trim() || !recordForm.province.trim()) throw new Error("请填写院校名称和省份");
    return {
      name: recordForm.name.trim(),
      province: recordForm.province.trim(),
      tier: recordForm.tier || null,
      is985: recordForm.is985,
      is211: recordForm.isDoubleFirstClass || recordForm.is211,
      isDoubleFirstClass: recordForm.isDoubleFirstClass,
      tags: recordForm.tags || null
    };
  }
  if (section.value === "majors") {
    if (!recordForm.name.trim()) throw new Error("请填写专业名称");
    return {
      name: recordForm.name.trim(),
      category: recordForm.category || null,
      degreeType: recordForm.degreeType || null,
      tags: recordForm.tags || null,
      subjectRequirement: recordForm.subjectRequirement || null,
      description: recordForm.description || null
    };
  }
  if (!recordForm.universityId || !recordForm.admissionYear || !recordForm.province.trim() || !recordForm.subjectType) {
    throw new Error("请完整填写院校、年份、省份和科类");
  }
  const base = {
    universityId: Number(recordForm.universityId),
    admissionYear: Number(recordForm.admissionYear),
    province: recordForm.province.trim(),
    subjectType: recordForm.subjectType,
    cutoffScore: nullableNumber(recordForm.cutoffScore),
    minRank: nullableNumber(recordForm.minRank)
  };
  if (section.value === "majorCutoffs") {
    if (!recordForm.majorName.trim()) throw new Error("请填写专业名称");
    return { ...base, majorId: nullableNumber(recordForm.majorId), majorName: recordForm.majorName.trim() };
  }
  return base;
}

async function saveRecord() {
  recordSubmitting.value = true;
  try {
    const paths = {
      universities: "/api/admin/universities",
      majors: "/api/admin/majors",
      cutoffs: "/api/admin/admission-cutoffs",
      majorCutoffs: "/api/admin/major-admission-cutoffs"
    };
    const base = paths[section.value];
    const url = editingId.value ? `${base}/${editingId.value}` : base;
    await api(url, {
      method: editingId.value ? "PUT" : "POST",
      body: JSON.stringify(buildRecordRequest())
    });
    recordDialogVisible.value = false;
    ElMessage.success(editingId.value ? "数据已更新" : "数据已新增");
    await loadSection();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    recordSubmitting.value = false;
  }
}

watch(section, loadSection);
onMounted(loadSection);
</script>

<template>
  <div class="admin-console">
    <aside class="admin-console__sidebar">
      <div class="admin-console__brand">
        <BrandLockup admin />
      </div>

      <nav class="admin-console__nav" aria-label="管理员导航">
        <section v-for="group in NAV_GROUPS" :key="group.label" class="admin-console__nav-group">
          <p>{{ group.label }}</p>
          <button
            v-for="item in group.items"
            :key="item.section"
            type="button"
            class="admin-console__nav-item"
            :class="{ 'is-active': section === item.section }"
            :aria-current="section === item.section ? 'page' : undefined"
            @click="goSection(item.section)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </button>
        </section>
      </nav>

      <div class="admin-console__connection">
        <span class="admin-console__connection-dot"></span>
        <div>
          <strong>业务数据已连接</strong>
          <small>
            <template v-if="platformSnapshot.latestYear">最新录取年份 {{ platformSnapshot.latestYear }}</template>
            <template v-else>进入工作台查看数据状态</template>
          </small>
        </div>
      </div>
      <img class="admin-console__art" :src="sidebarArt" alt="" />
    </aside>

    <section class="admin-console__workspace">
      <header class="admin-console__header">
        <div class="admin-console__heading">
          <span>管理后台 / {{ sectionMeta.title }}</span>
          <h1>{{ sectionMeta.title }}</h1>
          <p>{{ sectionMeta.subtitle }}</p>
        </div>
        <div class="admin-console__header-actions">
          <button class="admin-console__icon-button" type="button" title="刷新当前数据" aria-label="刷新当前数据" :disabled="loading" @click="loadSection">
            <el-icon :class="{ 'is-spinning': loading }"><Refresh /></el-icon>
          </button>
          <div class="admin-console__identity">
            <span class="admin-console__avatar">{{ currentUserInitial }}</span>
            <div><strong>{{ currentUsername }}</strong><small>系统管理员</small></div>
          </div>
          <button class="admin-console__logout" type="button" @click="logout">
            <el-icon><SwitchButton /></el-icon>退出
          </button>
        </div>
      </header>

      <main v-loading="loading" class="admin-console__content">
        <template v-if="section === 'dashboard'">
          <section class="admin-dashboard__hero">
            <div>
              <span class="admin-dashboard__eyebrow">ADMIN WORKSPACE</span>
              <h2>{{ currentUsername }}，欢迎进入管理工作台</h2>
              <p>所有指标均由现有管理接口实时汇总；这里不提供脱离业务的数据或空白配置项。</p>
            </div>
            <div class="admin-dashboard__sync">
              <span><i></i>实时业务数据</span>
              <small>最近同步 {{ lastSyncedAt || '—' }}</small>
            </div>
          </section>

          <section class="admin-dashboard__metrics" aria-label="平台核心指标">
            <button type="button" @click="goSection('users')">
              <span class="admin-dashboard__metric-icon"><el-icon><User /></el-icon></span>
              <div><small>用户总数</small><strong>{{ overview.totalCount || 0 }}</strong><em>{{ overview.adminCount || 0 }} 名管理员</em></div>
            </button>
            <button type="button" @click="goSection('universities')">
              <span class="admin-dashboard__metric-icon"><el-icon><School /></el-icon></span>
              <div><small>院校主数据</small><strong>{{ universities.length }}</strong><em>{{ universities.length ? `${dataHealth.universityCoverage}% 已关联录取数据` : '待录入院校主数据' }}</em></div>
            </button>
            <button type="button" @click="goSection('majors')">
              <span class="admin-dashboard__metric-icon"><el-icon><Reading /></el-icon></span>
              <div><small>专业主数据</small><strong>{{ majors.length }}</strong><em>{{ majors.length ? `${dataHealth.majorCoverage}% 已关联专业录取线` : '待录入专业主数据' }}</em></div>
            </button>
            <button type="button" @click="goSection('cutoffs')">
              <span class="admin-dashboard__metric-icon"><el-icon><TrendCharts /></el-icon></span>
              <div><small>录取事实数据</small><strong>{{ cutoffs.length + majorCutoffs.length }}</strong><em>覆盖 {{ platformSnapshot.provinceCount }} 个招生省份</em></div>
            </button>
          </section>

          <section class="admin-dashboard__grid">
            <article class="admin-dashboard__panel admin-dashboard__panel--business">
              <header><div><h3>业务运行</h3><p>来自用户推荐、志愿方案与 AI 会话计数</p></div><el-icon><DataAnalysis /></el-icon></header>
              <div class="admin-dashboard__business-stats">
                <div><span>推荐记录</span><strong>{{ formatNumber(businessTotals.recommendations) }}</strong><small>规则推荐与自然语言推荐</small></div>
                <div><span>志愿方案</span><strong>{{ formatNumber(businessTotals.plans) }}</strong><small>用户已保存方案</small></div>
                <div><span>AI 会话</span><strong>{{ formatNumber(businessTotals.conversations) }}</strong><small>受控 Agent 会话</small></div>
              </div>
            </article>

            <article class="admin-dashboard__panel">
              <header><div><h3>数据准备度</h3><p>按当前数据库关联关系计算</p></div><el-icon><TrendCharts /></el-icon></header>
              <div class="admin-dashboard__progress-list">
                <div><span>院校录取数据覆盖</span><b>{{ dataHealth.universityCoverage }}%</b><el-progress :percentage="dataHealth.universityCoverage" :show-text="false" /></div>
                <div><span>专业录取数据覆盖</span><b>{{ dataHealth.majorCoverage }}%</b><el-progress :percentage="dataHealth.majorCoverage" :show-text="false" status="warning" /></div>
                <div><span>普通用户档案完整度</span><b>{{ dataHealth.profileCompletion }}%</b><el-progress :percentage="dataHealth.profileCompletion" :show-text="false" status="success" /></div>
              </div>
            </article>
          </section>

          <section class="admin-dashboard__grid admin-dashboard__grid--bottom">
            <article class="admin-dashboard__panel">
              <header><div><h3>待处理事项</h3><p>由真实缺口自动生成，可直接进入对应模块</p></div><el-icon><Search /></el-icon></header>
              <div class="admin-dashboard__attention-list">
                <button v-for="item in attentionItems" :key="item.label" type="button" @click="goSection(item.section)">
                  <span class="admin-dashboard__attention-status" :class="`is-${item.tone}`"></span>
                  <div><strong>{{ item.label }}</strong><small>{{ item.detail }}</small></div>
                  <b>{{ item.value }}</b>
                </button>
              </div>
            </article>

            <article class="admin-dashboard__panel">
              <header>
                <div><h3>最新院校录取数据</h3><p>按录取年份与记录编号排序</p></div>
                <button type="button" @click="goSection('cutoffs')">查看全部</button>
              </header>
              <div v-if="recentCutoffs.length" class="admin-dashboard__recent-list">
                <div v-for="row in recentCutoffs" :key="row.id">
                  <span class="admin-dashboard__year">{{ row.admissionYear }}</span>
                  <div><strong>{{ universityName(row.universityId) }}</strong><small>{{ row.province }} · {{ subjectLabel(row.subjectType) }}</small></div>
                  <p><b>{{ row.cutoffScore ?? '—' }}</b> 分<small>位次 {{ formatNumber(row.minRank) }}</small></p>
                </div>
              </div>
              <el-empty v-else description="暂无院校录取线，请先录入事实数据" :image-size="70" />
            </article>
          </section>
        </template>

        <template v-else-if="section === 'users'">
          <section class="admin-user-overview" aria-label="用户概览">
            <div><span><el-icon><User /></el-icon></span><p>用户总数<strong>{{ overview.totalCount || 0 }}</strong></p></div>
            <div><span><el-icon><UserFilled /></el-icon></span><p>普通用户<strong>{{ overview.userCount || 0 }}</strong></p></div>
            <div><span><el-icon><Lock /></el-icon></span><p>管理员<strong>{{ overview.adminCount || 0 }}</strong></p></div>
            <div><span><el-icon><CircleCloseFilled /></el-icon></span><p>已停用<strong>{{ overview.disabledCount || 0 }}</strong></p></div>
          </section>

          <section class="admin-filter-panel">
            <label><span>用户名</span><el-input v-model.trim="userFilters.keyword" clearable placeholder="请输入用户名" @keyup.enter="loadSection" /></label>
            <label><span>角色</span><el-select v-model="userFilters.role"><el-option label="全部" value="" /><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></label>
            <label><span>账号状态</span><el-select v-model="userFilters.enabled"><el-option label="全部" value="" /><el-option label="正常" value="true" /><el-option label="已停用" value="false" /></el-select></label>
            <div class="admin-filter-panel__actions"><el-button type="primary" :icon="Search" @click="loadSection">查询</el-button><el-button @click="resetUserFilters">重置</el-button></div>
          </section>

          <section class="admin-table-panel admin-table-panel--content">
            <header><div><h3>用户列表</h3><p>角色与账号状态修改立即作用于鉴权</p></div><span>共 {{ users.length }} 条</span></header>
            <el-table :data="pagedUsers" max-height="560">
              <el-table-column prop="username" label="用户名" min-width="130" fixed="left" />
              <el-table-column label="报考资料" min-width="210"><template #default="{ row }"><span :class="{ 'admin-text--muted': profileLabel(row) === '未完善' }">{{ profileLabel(row) }}</span></template></el-table-column>
              <el-table-column label="角色" width="110"><template #default="{ row }"><el-tag effect="plain" :type="row.role === 'ADMIN' ? 'primary' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag></template></el-table-column>
              <el-table-column prop="recommendationCount" label="推荐记录" width="100" align="center" />
              <el-table-column prop="planCount" label="志愿方案" width="100" align="center" />
              <el-table-column prop="conversationCount" label="AI 会话" width="95" align="center" />
              <el-table-column label="注册时间" min-width="170"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column>
              <el-table-column label="状态" width="95"><template #default="{ row }"><el-tag :type="row.enabled === false ? 'warning' : 'success'" effect="light">{{ row.enabled === false ? '已停用' : '正常' }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="96" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="EditPen" @click="openUserSettings(row)">管理</el-button></template></el-table-column>
            </el-table>
            <el-pagination v-model:current-page="userPage" :page-size="pageSize" :total="users.length" layout="total, prev, pager, next" />
          </section>
        </template>

        <template v-else-if="section === 'universities'">
          <section class="admin-filter-panel admin-filter-panel--entity">
            <label><span>院校名称 / 省份</span><el-input v-model.trim="universityKeyword" clearable placeholder="请输入院校名称或省份" @keyup.enter="universityPage = 1" /></label>
            <label><span>院校层次</span><el-select v-model="universityTier" clearable placeholder="全部"><el-option v-for="tier in universityTiers" :key="tier" :label="tier" :value="tier" /></el-select></label>
            <div class="admin-filter-panel__actions"><el-button type="primary" :icon="Search" @click="universityPage = 1">查询</el-button><el-button @click="resetUniversityFilters">重置</el-button></div>
            <el-button class="admin-create-button" type="primary" :icon="Plus" @click="openRecordDialog()">新增院校</el-button>
          </section>
          <section class="admin-table-panel admin-table-panel--content">
            <header><div><h3>院校主数据</h3><p>用于院校查询、推荐与录取线关联；211 按本分支规则并入双一流展示</p></div><span>筛选后 {{ filteredUniversities.length }} 条</span></header>
            <el-table :data="pagedUniversities" max-height="610">
              <el-table-column prop="name" label="院校名称" min-width="210" fixed="left" />
              <el-table-column prop="province" label="省份" width="110" />
              <el-table-column prop="tier" label="院校层次" width="120" />
              <el-table-column label="985" width="80" align="center"><template #default="{ row }"><el-tag :type="row.is985 ? 'success' : 'info'" effect="light">{{ row.is985 ? '是' : '否' }}</el-tag></template></el-table-column>
              <el-table-column label="双一流" width="95" align="center"><template #default="{ row }"><el-tag :type="row.is211 || row.isDoubleFirstClass ? 'success' : 'info'" effect="light">{{ row.is211 || row.isDoubleFirstClass ? '是' : '否' }}</el-tag></template></el-table-column>
              <el-table-column prop="tags" label="标签" min-width="240" show-overflow-tooltip />
              <el-table-column label="操作" width="96" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="EditPen" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column>
            </el-table>
            <el-pagination v-model:current-page="universityPage" :page-size="pageSize" :total="filteredUniversities.length" layout="total, prev, pager, next" />
          </section>
        </template>

        <template v-else-if="section === 'majors'">
          <section class="admin-filter-panel admin-filter-panel--major">
            <label><span>专业名称 / 类别</span><el-input v-model.trim="majorKeyword" clearable placeholder="请输入专业名称或类别" @keyup.enter="majorPage = 1" /></label>
            <label><span>专业类别</span><el-select v-model="majorCategory" clearable placeholder="全部"><el-option v-for="category in majorCategories" :key="category" :label="category" :value="category" /></el-select></label>
            <label><span>学位类型</span><el-select v-model="majorDegreeType" clearable placeholder="全部"><el-option v-for="degree in majorDegreeTypes" :key="degree" :label="degree" :value="degree" /></el-select></label>
            <div class="admin-filter-panel__actions"><el-button type="primary" :icon="Search" @click="majorPage = 1">查询</el-button><el-button @click="resetMajorFilters">重置</el-button></div>
            <el-button class="admin-create-button" type="primary" :icon="Plus" @click="openRecordDialog()">新增专业</el-button>
          </section>
          <section class="admin-table-panel admin-table-panel--content">
            <header><div><h3>专业主数据</h3><p>专业录取线可通过专业 ID 或专业名称关联到这里</p></div><span>筛选后 {{ filteredMajors.length }} 条</span></header>
            <el-table :data="pagedMajors" max-height="610">
              <el-table-column prop="name" label="专业名称" min-width="190" fixed="left" />
              <el-table-column prop="category" label="专业类别" width="140" />
              <el-table-column prop="degreeType" label="学位类型" width="120" />
              <el-table-column prop="subjectRequirement" label="选科要求" min-width="160" show-overflow-tooltip />
              <el-table-column prop="tags" label="标签" min-width="160" show-overflow-tooltip />
              <el-table-column prop="description" label="专业说明" min-width="260" show-overflow-tooltip />
              <el-table-column label="操作" width="96" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="EditPen" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column>
            </el-table>
            <el-pagination v-model:current-page="majorPage" :page-size="pageSize" :total="filteredMajors.length" layout="total, prev, pager, next" />
          </section>
        </template>

        <template v-else-if="section === 'cutoffs'">
          <section class="admin-filter-panel admin-filter-panel--records">
            <label><span>院校</span><el-select v-model="cutoffFilters.universityId" filterable clearable placeholder="请选择院校"><el-option v-for="item in universities" :key="item.id" :label="item.name" :value="item.id" /></el-select></label>
            <label><span>年份</span><el-input v-model="cutoffFilters.admissionYear" clearable placeholder="请输入年份" /></label>
            <label><span>省份</span><el-input v-model.trim="cutoffFilters.province" clearable placeholder="请输入省份" /></label>
            <label><span>科类</span><el-select v-model="cutoffFilters.subjectType" clearable placeholder="全部"><el-option label="物理类" value="PHYSICS" /><el-option label="历史类" value="HISTORY" /></el-select></label>
            <div class="admin-filter-panel__actions"><el-button type="primary" :icon="Search" @click="loadSection">查询</el-button><el-button @click="resetCutoffFilters">重置</el-button></div>
            <el-button class="admin-create-button" type="primary" :icon="Plus" @click="openRecordDialog()">新增录取线</el-button>
          </section>
          <section class="admin-table-panel admin-table-panel--content">
            <header><div><h3>院校录取事实数据</h3><p>推荐服务优先使用数据库录取线；缺失值保持为空，不制造默认分数</p></div><span>当前返回 {{ cutoffs.length }} 条</span></header>
            <el-table :data="pagedCutoffs" max-height="610">
              <el-table-column label="院校" min-width="220" fixed="left"><template #default="{ row }">{{ universityName(row.universityId) }}</template></el-table-column>
              <el-table-column prop="admissionYear" label="年份" width="100" />
              <el-table-column prop="province" label="招生省份" width="130" />
              <el-table-column label="科类" width="120"><template #default="{ row }">{{ subjectLabel(row.subjectType) }}</template></el-table-column>
              <el-table-column label="最低分" width="110"><template #default="{ row }">{{ row.cutoffScore ?? '—' }}</template></el-table-column>
              <el-table-column label="最低位次" width="140"><template #default="{ row }">{{ formatNumber(row.minRank) }}</template></el-table-column>
              <el-table-column label="操作" width="96" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="EditPen" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column>
            </el-table>
            <el-pagination v-model:current-page="cutoffPage" :page-size="pageSize" :total="cutoffs.length" layout="total, prev, pager, next" />
          </section>
        </template>

        <template v-else-if="section === 'majorCutoffs'">
          <section class="admin-filter-panel admin-filter-panel--major-cutoff">
            <label><span>院校</span><el-select v-model="majorCutoffFilters.universityId" filterable clearable placeholder="请选择院校"><el-option v-for="item in universities" :key="item.id" :label="item.name" :value="item.id" /></el-select></label>
            <label><span>专业关键词</span><el-input v-model.trim="majorCutoffFilters.majorKeyword" clearable placeholder="请输入专业关键词" /></label>
            <label><span>年份</span><el-input v-model="majorCutoffFilters.admissionYear" clearable placeholder="年份" /></label>
            <label><span>省份</span><el-input v-model.trim="majorCutoffFilters.province" clearable placeholder="省份" /></label>
            <label><span>科类</span><el-select v-model="majorCutoffFilters.subjectType" clearable placeholder="全部"><el-option label="物理类" value="PHYSICS" /><el-option label="历史类" value="HISTORY" /></el-select></label>
            <div class="admin-filter-panel__actions"><el-button type="primary" :icon="Search" @click="loadSection">查询</el-button><el-button @click="resetMajorCutoffFilters">重置</el-button></div>
            <el-button class="admin-create-button" type="primary" :icon="Plus" @click="openRecordDialog()">新增专业录取线</el-button>
          </section>
          <section class="admin-table-panel admin-table-panel--content">
            <header><div><h3>专业录取事实数据</h3><p>数据同时关联院校与专业，供专业优先推荐链路使用</p></div><span>当前返回 {{ majorCutoffs.length }} 条</span></header>
            <el-table :data="pagedMajorCutoffs" max-height="610">
              <el-table-column label="院校" min-width="190" fixed="left"><template #default="{ row }">{{ universityName(row.universityId) }}</template></el-table-column>
              <el-table-column prop="majorName" label="专业名称" min-width="190" />
              <el-table-column prop="admissionYear" label="年份" width="90" />
              <el-table-column prop="province" label="招生省份" width="110" />
              <el-table-column label="科类" width="110"><template #default="{ row }">{{ subjectLabel(row.subjectType) }}</template></el-table-column>
              <el-table-column label="最低分" width="100"><template #default="{ row }">{{ row.cutoffScore ?? '—' }}</template></el-table-column>
              <el-table-column label="最低位次" width="130"><template #default="{ row }">{{ formatNumber(row.minRank) }}</template></el-table-column>
              <el-table-column label="操作" width="96" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="EditPen" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column>
            </el-table>
            <el-pagination v-model:current-page="majorCutoffPage" :page-size="pageSize" :total="majorCutoffs.length" layout="total, prev, pager, next" />
          </section>
        </template>
      </main>
    </section>
  </div>

  <el-dialog v-model="settingsVisible" title="用户设置" width="500px" destroy-on-close>
    <div v-if="selectedUser" class="admin-dialog-form">
      <dl>
        <div><dt>用户名</dt><dd>{{ selectedUser.username }}</dd></div>
        <div><dt>报考资料</dt><dd>{{ profileLabel(selectedUser) }}</dd></div>
        <div><dt>使用概况</dt><dd>推荐 {{ selectedUser.recommendationCount || 0 }} · 志愿方案 {{ selectedUser.planCount || 0 }} · AI 会话 {{ selectedUser.conversationCount || 0 }}</dd></div>
      </dl>
      <label><span>角色</span><el-select v-model="settingsForm.role" :disabled="selectedUser.username === currentUsername"><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></label>
      <label><span>账号状态</span><el-switch v-model="settingsForm.enabled" :disabled="selectedUser.username === currentUsername" active-text="正常" inactive-text="已停用" /></label>
      <p v-if="selectedUser.username === currentUsername" class="admin-dialog-form__note">为避免管理权限中断，当前管理员不能停用或降级自己的账号。</p>
    </div>
    <template #footer><el-button @click="settingsVisible = false">取消</el-button><el-button type="primary" :loading="settingsSubmitting" @click="saveUserSettings">保存设置</el-button></template>
  </el-dialog>

  <el-dialog v-model="recordDialogVisible" :title="recordDialogTitle" width="640px" destroy-on-close>
    <div class="admin-record-form">
      <template v-if="section === 'universities'">
        <label><span>院校名称</span><el-input v-model.trim="recordForm.name" placeholder="请输入标准院校名称" /></label>
        <label><span>省份</span><el-input v-model.trim="recordForm.province" placeholder="例如：浙江" /></label>
        <label><span>院校层次</span><el-input v-model.trim="recordForm.tier" placeholder="例如：985" /></label>
        <label class="admin-record-form__wide"><span>院校标签</span><el-input v-model.trim="recordForm.tags" placeholder="多个标签使用逗号分隔" /></label>
        <div class="admin-record-form__wide admin-record-form__checks"><el-checkbox v-model="recordForm.is985">985</el-checkbox><el-checkbox v-model="recordForm.isDoubleFirstClass">双一流（含 211）</el-checkbox></div>
      </template>
      <template v-else-if="section === 'majors'">
        <label><span>专业名称</span><el-input v-model.trim="recordForm.name" /></label>
        <label><span>专业类别</span><el-input v-model.trim="recordForm.category" /></label>
        <label><span>学位类型</span><el-input v-model.trim="recordForm.degreeType" /></label>
        <label><span>选科要求</span><el-input v-model.trim="recordForm.subjectRequirement" /></label>
        <label class="admin-record-form__wide"><span>标签</span><el-input v-model.trim="recordForm.tags" /></label>
        <label class="admin-record-form__wide"><span>专业说明</span><el-input v-model.trim="recordForm.description" type="textarea" :rows="3" /></label>
      </template>
      <template v-else>
        <label><span>院校</span><el-select v-model="recordForm.universityId" filterable><el-option v-for="item in universities" :key="item.id" :label="item.name" :value="item.id" /></el-select></label>
        <label v-if="section === 'majorCutoffs'"><span>专业名称</span><el-input v-model.trim="recordForm.majorName" /></label>
        <label><span>年份</span><el-input-number v-model="recordForm.admissionYear" :min="2000" :max="2100" /></label>
        <label><span>省份</span><el-input v-model.trim="recordForm.province" /></label>
        <label><span>科类</span><el-select v-model="recordForm.subjectType"><el-option label="物理类" value="PHYSICS" /><el-option label="历史类" value="HISTORY" /></el-select></label>
        <label><span>最低分</span><el-input-number v-model="recordForm.cutoffScore" :min="0" :max="750" /></label>
        <label><span>最低位次</span><el-input-number v-model="recordForm.minRank" :min="1" /></label>
      </template>
    </div>
    <template #footer><el-button @click="recordDialogVisible = false">取消</el-button><el-button type="primary" :loading="recordSubmitting" @click="saveRecord">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.admin-console {
  --admin-accent: #f47721;
  --admin-accent-dark: #db5f13;
  --admin-blue: #2f6fed;
  --admin-ink: #1d2939;
  --admin-muted: #667085;
  --admin-border: #e7ebf1;
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr);
  height: 100vh;
  min-height: 720px;
  overflow: hidden;
  color: var(--admin-ink);
  background: #f7f8fb;
}
.admin-console__sidebar { position: relative; display: flex; flex-direction: column; min-width: 0; padding: 24px 18px 0; overflow: hidden; border-right: 1px solid #e8e4df; background: linear-gradient(180deg, #fffaf5 0%, #fff 48%, #eef4ff 100%); }
.admin-console__brand { display: flex; align-items: center; min-height: 42px; padding: 0 8px; }
.admin-console__nav { position: relative; z-index: 2; display: grid; gap: 22px; margin-top: 30px; }
.admin-console__nav-group > p { margin: 0 0 7px 14px; color: #a0a7b4; font-size: 11px; font-weight: 700; letter-spacing: 0.12em; }
.admin-console__nav-item { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 46px; padding: 0 14px; border: 0; border-radius: 11px; color: #657187; background: transparent; font: inherit; font-size: 14px; text-align: left; cursor: pointer; transition: color 0.18s ease, background 0.18s ease, transform 0.18s ease; }
.admin-console__nav-item .el-icon { font-size: 19px; }
.admin-console__nav-item:hover { color: var(--admin-accent-dark); background: #fff1e7; transform: translateX(2px); }
.admin-console__nav-item.is-active { color: var(--admin-accent-dark); background: linear-gradient(90deg, #ffe8d7 0%, #fff3ea 100%); font-weight: 650; box-shadow: inset 3px 0 0 var(--admin-accent); }
.admin-console__connection { position: relative; z-index: 2; display: flex; align-items: flex-start; gap: 10px; margin: auto 4px 18px; padding: 12px 13px; border: 1px solid rgba(47, 111, 237, 0.13); border-radius: 12px; background: rgba(255, 255, 255, 0.82); backdrop-filter: blur(8px); }
.admin-console__connection-dot { flex: 0 0 auto; width: 8px; height: 8px; margin-top: 5px; border-radius: 50%; background: #20b26b; box-shadow: 0 0 0 4px rgba(32, 178, 107, 0.12); }
.admin-console__connection div { display: grid; gap: 4px; min-width: 0; }
.admin-console__connection strong { color: #344054; font-size: 12px; }
.admin-console__connection small { overflow: hidden; color: #8a94a6; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.admin-console__art { position: absolute; z-index: 0; right: -35px; bottom: -48px; width: 265px; opacity: 0.4; pointer-events: none; mask-image: linear-gradient(to bottom, transparent, #000 28%); }
.admin-console__workspace { display: grid; grid-template-rows: 78px minmax(0, 1fr); min-width: 0; min-height: 0; }
.admin-console__header { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 0 28px; border-bottom: 1px solid var(--admin-border); background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(12px); }
.admin-console__heading { min-width: 0; }
.admin-console__heading > span { display: none; color: #98a2b3; font-size: 11px; }
.admin-console__heading h1 { margin: 0; color: #1d2939; font-size: 22px; line-height: 1.25; }
.admin-console__heading p { margin: 5px 0 0; color: #8a94a6; font-size: 12px; }
.admin-console__header-actions { display: flex; align-items: center; gap: 14px; }
.admin-console__icon-button { display: grid; place-items: center; width: 36px; height: 36px; border: 1px solid #e3e8ef; border-radius: 10px; color: #667085; background: #fff; cursor: pointer; }
.admin-console__icon-button:hover { color: var(--admin-blue); border-color: #bed0f7; }
.admin-console__icon-button:disabled { cursor: wait; opacity: 0.65; }
.admin-console__identity { display: flex; align-items: center; gap: 10px; min-width: 150px; padding-left: 14px; border-left: 1px solid #eaecf0; }
.admin-console__avatar { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 50%; color: #fff; background: linear-gradient(135deg, #ffab70, var(--admin-accent)); font-size: 15px; font-weight: 700; box-shadow: 0 7px 16px rgba(244, 119, 33, 0.2); }
.admin-console__identity div { display: grid; gap: 2px; }
.admin-console__identity strong { color: #344054; font-size: 13px; }
.admin-console__identity small { color: #98a2b3; font-size: 10px; }
.admin-console__logout { display: inline-flex; align-items: center; gap: 6px; min-height: 34px; padding: 0 11px; border: 0; color: #667085; background: transparent; font: inherit; font-size: 12px; cursor: pointer; }
.admin-console__logout:hover { color: #d92d20; }
.admin-console__content { min-height: 0; padding: 24px 28px 36px; overflow: auto; }
.admin-dashboard__hero { display: flex; align-items: center; justify-content: space-between; gap: 28px; padding: 24px 26px; border: 1px solid #f1dfd2; border-radius: 16px; background: linear-gradient(120deg, #fff9f4 0%, #fff 55%, #f1f6ff 100%); box-shadow: 0 8px 28px rgba(45, 55, 72, 0.035); }
.admin-dashboard__eyebrow { color: var(--admin-accent); font-size: 11px; font-weight: 800; letter-spacing: 0.13em; }
.admin-dashboard__hero h2 { margin: 7px 0 0; font-size: 24px; line-height: 1.3; }
.admin-dashboard__hero p { margin: 8px 0 0; color: var(--admin-muted); font-size: 13px; line-height: 1.7; }
.admin-dashboard__sync { display: grid; justify-items: end; gap: 7px; flex: 0 0 auto; }
.admin-dashboard__sync span { display: inline-flex; align-items: center; gap: 7px; padding: 7px 11px; border-radius: 999px; color: #18794e; background: #ecfdf3; font-size: 12px; font-weight: 600; }
.admin-dashboard__sync i { width: 7px; height: 7px; border-radius: 50%; background: #20b26b; }
.admin-dashboard__sync small { color: #98a2b3; font-size: 11px; }
.admin-dashboard__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-top: 18px; }
.admin-dashboard__metrics > button { display: flex; align-items: center; gap: 15px; min-width: 0; min-height: 112px; padding: 20px; border: 1px solid var(--admin-border); border-radius: 14px; color: inherit; background: #fff; text-align: left; cursor: pointer; box-shadow: 0 7px 22px rgba(16, 24, 40, 0.025); transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease; }
.admin-dashboard__metrics > button:hover { transform: translateY(-2px); border-color: #fac9a7; box-shadow: 0 12px 28px rgba(55, 65, 81, 0.07); }
.admin-dashboard__metric-icon { display: grid; place-items: center; flex: 0 0 auto; width: 46px; height: 46px; border-radius: 13px; color: var(--admin-accent); background: #fff2e8; font-size: 22px; }
.admin-dashboard__metrics button:nth-child(2) .admin-dashboard__metric-icon { color: #2f6fed; background: #edf3ff; }
.admin-dashboard__metrics button:nth-child(3) .admin-dashboard__metric-icon { color: #7f56d9; background: #f4f0ff; }
.admin-dashboard__metrics button:nth-child(4) .admin-dashboard__metric-icon { color: #039855; background: #ecfdf3; }
.admin-dashboard__metrics button > div { display: grid; min-width: 0; }
.admin-dashboard__metrics small { color: #667085; font-size: 12px; }
.admin-dashboard__metrics strong { margin-top: 3px; color: #101828; font-size: 27px; line-height: 1.2; }
.admin-dashboard__metrics em { overflow: hidden; margin-top: 5px; color: #98a2b3; font-size: 10px; font-style: normal; text-overflow: ellipsis; white-space: nowrap; }
.admin-dashboard__grid { display: grid; grid-template-columns: minmax(0, 1.18fr) minmax(360px, 0.82fr); gap: 16px; margin-top: 16px; }
.admin-dashboard__grid--bottom { grid-template-columns: minmax(0, 0.95fr) minmax(420px, 1.05fr); }
.admin-dashboard__panel { min-width: 0; padding: 21px 22px; border: 1px solid var(--admin-border); border-radius: 14px; background: #fff; box-shadow: 0 7px 22px rgba(16, 24, 40, 0.025); }
.admin-dashboard__panel > header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.admin-dashboard__panel > header h3 { margin: 0; font-size: 16px; }
.admin-dashboard__panel > header p { margin: 5px 0 0; color: #98a2b3; font-size: 11px; }
.admin-dashboard__panel > header > .el-icon { color: #98a2b3; font-size: 20px; }
.admin-dashboard__panel > header > button { border: 0; color: var(--admin-accent-dark); background: transparent; font: inherit; font-size: 12px; cursor: pointer; }
.admin-dashboard__business-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 20px; }
.admin-dashboard__business-stats > div { display: grid; min-width: 0; padding: 15px; border: 1px solid #edf0f4; border-radius: 11px; background: #fbfcfe; }
.admin-dashboard__business-stats span { color: #667085; font-size: 11px; }
.admin-dashboard__business-stats strong { margin-top: 8px; color: #1d2939; font-size: 24px; }
.admin-dashboard__business-stats small { overflow: hidden; margin-top: 4px; color: #98a2b3; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.admin-dashboard__progress-list { display: grid; gap: 16px; margin-top: 20px; }
.admin-dashboard__progress-list > div { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 7px 12px; }
.admin-dashboard__progress-list span { color: #475467; font-size: 12px; }
.admin-dashboard__progress-list b { color: #344054; font-size: 12px; }
.admin-dashboard__progress-list :deep(.el-progress) { grid-column: 1 / -1; }
.admin-dashboard__progress-list :deep(.el-progress-bar__outer) { height: 7px !important; background: #f0f2f5; }
.admin-dashboard__attention-list { display: grid; gap: 8px; margin-top: 17px; }
.admin-dashboard__attention-list > button { display: grid; grid-template-columns: 8px minmax(0, 1fr) auto; align-items: center; gap: 11px; width: 100%; padding: 11px 12px; border: 1px solid transparent; border-radius: 9px; color: inherit; background: #fafbfc; text-align: left; cursor: pointer; }
.admin-dashboard__attention-list > button:hover { border-color: #f2d2bd; background: #fffaf6; }
.admin-dashboard__attention-status { width: 8px; height: 8px; border-radius: 50%; background: #98a2b3; }
.admin-dashboard__attention-status.is-warning { background: #f79009; }
.admin-dashboard__attention-status.is-danger { background: #f04438; }
.admin-dashboard__attention-status.is-info { background: #2f6fed; }
.admin-dashboard__attention-status.is-success { background: #12b76a; }
.admin-dashboard__attention-list div { display: grid; gap: 3px; min-width: 0; }
.admin-dashboard__attention-list strong { color: #344054; font-size: 11px; }
.admin-dashboard__attention-list small { overflow: hidden; color: #98a2b3; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.admin-dashboard__attention-list b { color: #475467; font-size: 12px; }
.admin-dashboard__recent-list { display: grid; margin-top: 15px; }
.admin-dashboard__recent-list > div { display: grid; grid-template-columns: 46px minmax(0, 1fr) auto; align-items: center; gap: 11px; padding: 10px 0; border-bottom: 1px solid #f0f2f5; }
.admin-dashboard__recent-list > div:last-child { border-bottom: 0; }
.admin-dashboard__year { display: grid; place-items: center; min-height: 28px; border-radius: 7px; color: var(--admin-accent-dark); background: #fff2e8; font-size: 10px; font-weight: 700; }
.admin-dashboard__recent-list div > div { display: grid; gap: 3px; min-width: 0; }
.admin-dashboard__recent-list strong { overflow: hidden; color: #344054; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.admin-dashboard__recent-list small { color: #98a2b3; font-size: 9px; }
.admin-dashboard__recent-list p { display: grid; justify-items: end; gap: 2px; margin: 0; color: #667085; font-size: 10px; }
.admin-dashboard__recent-list p b { color: #1d2939; font-size: 14px; }
.admin-user-overview { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); min-height: 104px; padding: 0 18px; border: 1px solid var(--admin-border); border-radius: 14px; background: linear-gradient(110deg, #fffaf6 0%, #fff 48%, #f6f9ff 100%); }
.admin-user-overview > div { position: relative; display: flex; align-items: center; justify-content: center; gap: 14px; }
.admin-user-overview > div:not(:last-child)::after { position: absolute; top: 31px; right: 0; width: 1px; height: 42px; content: ""; background: #e7ebf1; }
.admin-user-overview > div > span { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; color: var(--admin-accent); background: #fff0e4; font-size: 20px; }
.admin-user-overview p { display: grid; gap: 3px; margin: 0; color: #667085; font-size: 12px; }
.admin-user-overview strong { color: var(--admin-accent-dark); font-size: 26px; }
.admin-filter-panel { display: grid; grid-template-columns: minmax(210px, 1.3fr) repeat(2, minmax(160px, 1fr)) auto; gap: 18px; align-items: end; margin-top: 16px; padding: 18px 20px; border: 1px solid var(--admin-border); border-radius: 14px; background: #fff; }
.admin-filter-panel--entity { grid-template-columns: minmax(250px, 1.6fr) minmax(160px, 0.8fr) auto 132px; }
.admin-filter-panel--major { grid-template-columns: minmax(220px, 1.4fr) repeat(2, minmax(145px, 0.8fr)) auto 132px; }
.admin-filter-panel--records { grid-template-columns: minmax(180px, 1.3fr) repeat(3, minmax(115px, 0.72fr)) auto 145px; }
.admin-filter-panel--major-cutoff { grid-template-columns: minmax(170px, 1fr) minmax(150px, 0.9fr) repeat(3, minmax(100px, 0.62fr)) auto 178px; }
.admin-filter-panel label, .admin-dialog-form label, .admin-record-form label { display: grid; gap: 7px; min-width: 0; color: #475467; font-size: 12px; font-weight: 600; }
.admin-filter-panel :deep(.el-select), .admin-dialog-form :deep(.el-select), .admin-record-form :deep(.el-select), .admin-record-form :deep(.el-input-number) { width: 100%; }
.admin-filter-panel :deep(.el-input__wrapper), .admin-filter-panel :deep(.el-select__wrapper) { min-height: 38px; border-radius: 8px; box-shadow: 0 0 0 1px #dfe4ea inset; }
.admin-filter-panel__actions { display: flex; gap: 8px; }
.admin-filter-panel__actions :deep(.el-button--primary), .admin-create-button { min-width: 92px; }
.admin-filter-panel :deep(.el-button--primary), .admin-create-button { --el-button-bg-color: var(--admin-accent); --el-button-border-color: var(--admin-accent); --el-button-hover-bg-color: #ff8b42; --el-button-hover-border-color: #ff8b42; }
.admin-table-panel { margin-top: 16px; overflow: hidden; border: 1px solid var(--admin-border); border-radius: 14px; background: #fff; }
.admin-table-panel > header { display: flex; align-items: center; justify-content: space-between; gap: 20px; min-height: 68px; padding: 0 20px; border-bottom: 1px solid #edf0f4; }
.admin-table-panel > header h3 { margin: 0; font-size: 15px; }
.admin-table-panel > header p { margin: 5px 0 0; color: #98a2b3; font-size: 10px; }
.admin-table-panel > header > span { flex: 0 0 auto; color: #98a2b3; font-size: 11px; }
.admin-table-panel :deep(.el-table) { --el-table-border-color: #edf0f4; --el-table-header-bg-color: #f8f9fb; --el-table-row-hover-bg-color: #fffaf6; }
.admin-table-panel :deep(.el-table th.el-table__cell) { height: 48px; color: #475467; font-size: 12px; }
.admin-table-panel :deep(.el-table td.el-table__cell) { height: 54px; color: #475467; font-size: 12px; }
.admin-table-panel :deep(.el-pagination) { justify-content: flex-end; padding: 14px 18px; border-top: 1px solid #edf0f4; }
.admin-table-panel :deep(.el-pagination .is-active) { background: var(--admin-accent) !important; }
.admin-text--muted { color: #98a2b3; }
.admin-dialog-form { display: grid; gap: 18px; }
.admin-dialog-form dl { display: grid; gap: 11px; margin: 0; padding: 15px; border-radius: 11px; background: #f8f9fb; }
.admin-dialog-form dl > div { display: grid; grid-template-columns: 82px minmax(0, 1fr); gap: 12px; }
.admin-dialog-form dt { color: #98a2b3; font-size: 12px; }
.admin-dialog-form dd { margin: 0; color: #344054; font-size: 12px; }
.admin-dialog-form__note { margin: -5px 0 0; color: #98a2b3; font-size: 11px; line-height: 1.6; }
.admin-record-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 17px 20px; }
.admin-record-form__wide { grid-column: 1 / -1; }
.admin-record-form__checks { display: flex; gap: 24px; padding-top: 2px; }
.admin-record-form :deep(.el-input__wrapper), .admin-record-form :deep(.el-select__wrapper), .admin-record-form :deep(.el-input-number) { min-height: 39px; border-radius: 8px; }
.is-spinning { animation: admin-spin 0.85s linear infinite; }
@keyframes admin-spin { to { transform: rotate(360deg); } }
@media (max-width: 1380px) {
  .admin-console { grid-template-columns: 232px minmax(0, 1fr); }
  .admin-console__content { padding: 20px; }
  .admin-dashboard__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .admin-dashboard__grid, .admin-dashboard__grid--bottom { grid-template-columns: 1fr; }
  .admin-filter-panel, .admin-filter-panel--entity, .admin-filter-panel--major, .admin-filter-panel--records, .admin-filter-panel--major-cutoff { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .admin-filter-panel__actions { justify-content: flex-end; }
  .admin-create-button { width: 100%; }
}
@media (max-width: 900px) {
  .admin-console { display: block; height: auto; min-height: 100vh; overflow: visible; }
  .admin-console__sidebar { height: auto; padding-bottom: 14px; border-right: 0; border-bottom: 1px solid #e8e4df; }
  .admin-console__nav { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 18px; }
  .admin-console__nav-group > p { margin-left: 8px; }
  .admin-console__connection, .admin-console__art { display: none; }
  .admin-console__workspace { display: block; }
  .admin-console__header { min-height: 78px; }
}
@media (max-width: 680px) {
  .admin-console__nav { grid-template-columns: 1fr; }
  .admin-console__header { align-items: flex-start; padding: 16px; }
  .admin-console__heading p, .admin-console__identity div, .admin-console__icon-button { display: none; }
  .admin-console__identity { min-width: auto; padding-left: 0; border-left: 0; }
  .admin-console__content { padding: 14px; }
  .admin-dashboard__hero { align-items: flex-start; flex-direction: column; }
  .admin-dashboard__sync { justify-items: start; }
  .admin-dashboard__metrics, .admin-user-overview, .admin-filter-panel, .admin-filter-panel--entity, .admin-filter-panel--major, .admin-filter-panel--records, .admin-filter-panel--major-cutoff, .admin-record-form { grid-template-columns: 1fr; }
  .admin-user-overview > div { min-height: 76px; }
  .admin-user-overview > div::after { display: none; }
  .admin-dashboard__business-stats { grid-template-columns: 1fr; }
  .admin-record-form__wide { grid-column: auto; }
}
</style>
