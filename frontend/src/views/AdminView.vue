<script setup>
import { ElMessage } from "element-plus";
import { ChatDotRound, CircleCloseFilled, Collection, DataAnalysis, Lock, OfficeBuilding, SwitchButton, User, UserFilled } from "@element-plus/icons-vue";
import { computed, inject, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import BrandLockup from "../components/BrandLockup.vue";
import sidebarArt from "../assets/admission-journey.png";

defineOptions({ name: "AdminView" });

const route = useRoute();
const router = useRouter();
const workspace = inject("workspace");

const SECTION_TITLES = {
  users: "用户管理",
  universities: "院校管理",
  majors: "专业管理",
  cutoffs: "院校录取线",
  majorCutoffs: "专业录取线",
  ai: "AI 管理"
};
const section = computed(() => SECTION_TITLES[String(route.query.section || "users")] ? String(route.query.section || "users") : "users");
const pageTitle = computed(() => SECTION_TITLES[section.value]);
const token = computed(() => workspace?.auth?.value?.token || "");
const currentUsername = computed(() => workspace?.auth?.value?.user?.username || "管理员");
const loading = ref(false);
const aiSaving = ref(false);
const aiTesting = ref(false);
const aiTestResult = ref(null);
const aiConfig = reactive({
  provider: "openai-compatible",
  baseUrl: "",
  model: "",
  apiKey: "",
  apiKeyConfigured: false,
  apiKeyMasked: "",
  apiKeySource: "none"
});

const overview = reactive({ totalCount: 0, userCount: 0, adminCount: 0, disabledCount: 0 });
const users = ref([]);
const userFilters = reactive({ keyword: "", role: "", enabled: "" });
const userPage = ref(1);
const pageSize = 10;
const usersTotal = ref(0);
const pagedUsers = computed(() => users.value);

const universities = ref([]);
const universityKeyword = ref("");
const universityTier = ref("");
const universityPage = ref(1);
const universityTiers = computed(() => [...new Set(universities.value.map((item) => item.tier).filter(Boolean))]);
const filteredUniversities = computed(() => {
  const keyword = universityKeyword.value.trim().toLowerCase();
  return universities.value.filter((item) => {
    const matchesKeyword = !keyword || `${item.name}${item.province}${item.tier || ""}`.toLowerCase().includes(keyword);
    return matchesKeyword && (!universityTier.value || item.tier === universityTier.value);
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
    const matchesKeyword = !keyword || `${item.name}${item.category || ""}`.toLowerCase().includes(keyword);
    return matchesKeyword
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
  universityId: "", majorId: "", majorName: "", admissionYear: 2025, subjectType: "PHYSICS", cutoffScore: "", minRank: ""
});

const recordDialogTitle = computed(() => {
  const labels = { universities: "院校", majors: "专业", cutoffs: "院校录取线", majorCutoffs: "专业录取线" };
  return `${editingId.value ? "编辑" : "新增"}${labels[section.value] || "数据"}`;
});

async function api(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${token.value}`,
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {})
    }
  });
  const data = response.headers.get("content-type")?.includes("application/json") ? await response.json() : null;
  if (!response.ok) throw new Error(data?.message || "操作失败，请稍后重试");
  return data;
}

function formatDate(value) {
  if (!value) return "—";
  return String(value).replace("T", " ").slice(0, 19);
}

function subjectLabel(value) {
  if (value === "PHYSICS" || value === "物理") return "物理类";
  if (value === "HISTORY" || value === "历史") return "历史类";
  return value || "";
}

function profileLabel(user) {
  if (!user?.examProvince || !user?.subjectType || user?.score == null) return "未完善";
  return `${user.examProvince} · ${subjectLabel(user.subjectType)} · ${user.score}`;
}

function universityName(id) {
  return universities.value.find((item) => Number(item.id) === Number(id))?.name || `院校 #${id}`;
}

async function loadUsers() {
  loading.value = true;
  try {
    const params = new URLSearchParams();
    if (userFilters.keyword.trim()) params.set("keyword", userFilters.keyword.trim());
    if (userFilters.role) params.set("role", userFilters.role);
    if (userFilters.enabled !== "") params.set("enabled", userFilters.enabled);
    params.set("page", String(userPage.value));
    params.set("size", String(pageSize));
    const countParams = new URLSearchParams(params);
    countParams.delete("page");
    countParams.delete("size");
    const [list, count, summary] = await Promise.all([
      api(`/api/admin/users?${params}`),
      api(`/api/admin/users/count${countParams.size ? `?${countParams}` : ""}`),
      api("/api/admin/users/overview")
    ]);
    users.value = Array.isArray(list) ? list : [];
    usersTotal.value = Number(count?.total || 0);
    Object.assign(overview, summary || {});
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}

function resetUserFilters() {
  Object.assign(userFilters, { keyword: "", role: "", enabled: "" });
  userPage.value = 1;
  loadUsers();
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
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    settingsSubmitting.value = false;
  }
}

async function loadUniversities() {
  universities.value = await api("/api/admin/universities");
  universityPage.value = 1;
}

async function loadMajors() {
  majors.value = await api("/api/admin/majors");
  majorPage.value = 1;
}

async function loadAiConfig() {
  const data = await api("/api/admin/ai-config");
  Object.assign(aiConfig, data || {}, { apiKey: "" });
}

function aiConfigPayload() {
  return {
    provider: aiConfig.provider.trim(),
    baseUrl: aiConfig.baseUrl.trim(),
    model: aiConfig.model.trim(),
    apiKey: aiConfig.apiKey,
    clearApiKey: false
  };
}

function validateAiConfigForm() {
  if (!aiConfig.provider.trim() || !aiConfig.baseUrl.trim() || !aiConfig.model.trim()) {
    ElMessage.warning("请完整填写提供方、接口地址和模型名称");
    return false;
  }
  return true;
}

async function testAiConfig() {
  if (!validateAiConfigForm()) return;
  aiTesting.value = true;
  aiTestResult.value = null;
  try {
    const data = await api("/api/admin/ai-config/test", {
      method: "POST",
      body: JSON.stringify(aiConfigPayload())
    });
    aiTestResult.value = data;
    if (data?.available) ElMessage.success("AI 接口检测通过");
    else ElMessage.warning(data?.message || "AI 接口当前不可用");
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    aiTesting.value = false;
  }
}

async function saveAiConfig() {
  if (!validateAiConfigForm()) return;
  aiSaving.value = true;
  try {
    const data = await api("/api/admin/ai-config", {
      method: "PUT",
      body: JSON.stringify(aiConfigPayload())
    });
    Object.assign(aiConfig, data || {}, { apiKey: "" });
    ElMessage.success("AI 配置已保存，后续请求将直接使用新配置");
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    aiSaving.value = false;
  }
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

function appendQuery(params, key, value) {
  if (value !== "" && value != null) params.set(key, value);
}

async function loadCutoffs() {
  loading.value = true;
  try {
    if (!universities.value.length) await loadUniversities();
    const params = new URLSearchParams();
    Object.entries(cutoffFilters).forEach(([key, value]) => appendQuery(params, key, value));
    cutoffs.value = await api(`/api/admin/admission-cutoffs${params.size ? `?${params}` : ""}`);
    cutoffPage.value = 1;
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}

async function loadMajorCutoffs() {
  loading.value = true;
  try {
    if (!universities.value.length) await loadUniversities();
    const params = new URLSearchParams();
    Object.entries(majorCutoffFilters).forEach(([key, value]) => appendQuery(params, key, value));
    majorCutoffs.value = await api(`/api/admin/major-admission-cutoffs${params.size ? `?${params}` : ""}`);
    majorCutoffPage.value = 1;
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}

function resetCutoffFilters() {
  Object.assign(cutoffFilters, { universityId: "", admissionYear: "", province: "", subjectType: "" });
  loadCutoffs();
}

function resetMajorCutoffFilters() {
  Object.assign(majorCutoffFilters, { universityId: "", admissionYear: "", province: "", subjectType: "", majorKeyword: "" });
  loadMajorCutoffs();
}

function resetRecordForm() {
  Object.assign(recordForm, {
    name: "", province: "", tier: "", is985: false, is211: false, isDoubleFirstClass: false, tags: "",
    category: "", degreeType: "", subjectRequirement: "", description: "",
    universityId: "", majorId: "", majorName: "", admissionYear: 2025, subjectType: "PHYSICS", cutoffScore: "", minRank: ""
  });
}

function openRecordDialog(record = null) {
  resetRecordForm();
  editingId.value = record?.id || null;
  if (record) Object.keys(recordForm).forEach((key) => {
    if (record[key] != null) recordForm[key] = record[key];
  });
  recordDialogVisible.value = true;
}

function nullableNumber(value) {
  return value === "" || value == null ? null : Number(value);
}

function buildRecordRequest() {
  if (section.value === "universities") {
    if (!recordForm.name.trim() || !recordForm.province.trim()) throw new Error("请填写院校名称和省份");
    return { name: recordForm.name.trim(), province: recordForm.province.trim(), tier: recordForm.tier || null,
      is985: recordForm.is985, is211: recordForm.is211, isDoubleFirstClass: recordForm.isDoubleFirstClass, tags: recordForm.tags || null };
  }
  if (section.value === "majors") {
    if (!recordForm.name.trim()) throw new Error("请填写专业名称");
    return { name: recordForm.name.trim(), category: recordForm.category || null, degreeType: recordForm.degreeType || null,
      tags: recordForm.tags || null, subjectRequirement: recordForm.subjectRequirement || null, description: recordForm.description || null };
  }
  if (!recordForm.universityId || !recordForm.admissionYear || !recordForm.province.trim() || !recordForm.subjectType) {
    throw new Error("请完整填写院校、年份、省份和科类");
  }
  const base = { universityId: Number(recordForm.universityId), admissionYear: Number(recordForm.admissionYear),
    province: recordForm.province.trim(), subjectType: recordForm.subjectType,
    cutoffScore: nullableNumber(recordForm.cutoffScore), minRank: nullableNumber(recordForm.minRank) };
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
    await api(url, { method: editingId.value ? "PUT" : "POST", body: JSON.stringify(buildRecordRequest()) });
    recordDialogVisible.value = false;
    ElMessage.success(editingId.value ? "数据已更新" : "数据已新增");
    await loadSection();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    recordSubmitting.value = false;
  }
}

async function loadSection() {
  loading.value = true;
  try {
    if (section.value === "users") return await loadUsers();
    if (section.value === "universities") return await loadUniversities();
    if (section.value === "majors") return await loadMajors();
    if (section.value === "cutoffs") return await loadCutoffs();
    if (section.value === "majorCutoffs") return await loadMajorCutoffs();
    if (section.value === "ai") return await loadAiConfig();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}

function navigateAdminSection(nextSection) {
  if (nextSection === section.value) return;
  router.push({ name: "admin", query: { section: nextSection } });
}

async function logout() {
  if (typeof workspace?.logout === "function") {
    await workspace.logout();
    return;
  }
  await router.replace({ name: "login" });
}

watch(section, loadSection);
watch(
  () => [aiConfig.provider, aiConfig.baseUrl, aiConfig.model, aiConfig.apiKey],
  () => { aiTestResult.value = null; }
);
onMounted(loadSection);
</script>

<template>
  <div class="app-layout app-layout--admin">
    <aside class="app-sidebar">
      <div class="app-brand"><BrandLockup :admin="true" /></div>
      <nav class="app-nav" aria-label="管理导航">
        <button class="app-nav__item" :class="{ 'is-active': section === 'users' }" type="button" @click="navigateAdminSection('users')"><el-icon><UserFilled /></el-icon><span>用户管理</span></button>
        <button class="app-nav__item" :class="{ 'is-active': section === 'universities' }" type="button" @click="navigateAdminSection('universities')"><el-icon><OfficeBuilding /></el-icon><span>院校管理</span></button>
        <button class="app-nav__item" :class="{ 'is-active': section === 'majors' }" type="button" @click="navigateAdminSection('majors')"><el-icon><Collection /></el-icon><span>专业管理</span></button>
        <button class="app-nav__item" :class="{ 'is-active': section === 'cutoffs' }" type="button" @click="navigateAdminSection('cutoffs')"><el-icon><DataAnalysis /></el-icon><span>院校录取线</span></button>
        <button class="app-nav__item" :class="{ 'is-active': section === 'majorCutoffs' }" type="button" @click="navigateAdminSection('majorCutoffs')"><el-icon><DataAnalysis /></el-icon><span>专业录取线</span></button>
        <button class="app-nav__item" :class="{ 'is-active': section === 'ai' }" type="button" @click="navigateAdminSection('ai')"><el-icon><ChatDotRound /></el-icon><span>AI 管理</span></button>
      </nav>
      <div class="app-sidebar__art" aria-hidden="true"><img :src="sidebarArt" alt="" /></div>
    </aside>
    <section class="app-content">
      <header class="app-header"><div class="app-header__content"><h1>{{ pageTitle }}</h1><div class="app-user"><span class="app-user__avatar"><el-icon><UserFilled /></el-icon></span><strong>{{ currentUsername }}</strong><span class="app-user__divider" /><button class="app-user__logout" type="button" @click="logout"><el-icon><SwitchButton /></el-icon><span>退出</span></button></div></div></header>
      <main class="admin-view">
    <template v-if="section === 'users'">
      <section class="admin-overview" aria-label="用户概览">
        <div><el-icon class="admin-overview__icon"><User /></el-icon><span>用户总数<strong>{{ overview.totalCount || 0 }}</strong></span></div>
        <div><el-icon class="admin-overview__icon"><UserFilled /></el-icon><span>普通用户<strong>{{ overview.userCount || 0 }}</strong></span></div>
        <div><el-icon class="admin-overview__icon"><Lock /></el-icon><span>管理员<strong>{{ overview.adminCount || 0 }}</strong></span></div>
        <div><el-icon class="admin-overview__icon"><CircleCloseFilled /></el-icon><span>已停用<strong>{{ overview.disabledCount || 0 }}</strong></span></div>
      </section>

      <section class="admin-filter-bar">
        <label><span>用户名</span><el-input v-model.trim="userFilters.keyword" clearable placeholder="请输入用户名" @keyup.enter="userPage = 1; loadUsers()" /></label>
        <label><span>角色</span><el-select v-model="userFilters.role"><el-option label="全部" value="" /><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></label>
        <label><span>账号状态</span><el-select v-model="userFilters.enabled"><el-option label="全部" value="" /><el-option label="正常" value="true" /><el-option label="已停用" value="false" /></el-select></label>
        <div class="admin-filter-actions"><el-button type="primary" @click="userPage = 1; loadUsers()">查询</el-button><el-button @click="resetUserFilters">重置</el-button></div>
      </section>

      <section class="admin-table-panel">
        <el-table v-loading="loading" :data="pagedUsers" height="100%">
          <el-table-column prop="username" label="用户名" min-width="130" />
          <el-table-column label="报考资料" min-width="200"><template #default="{ row }">{{ profileLabel(row) }}</template></el-table-column>
          <el-table-column label="角色" width="110"><template #default="{ row }"><el-tag effect="plain" :type="row.role === 'ADMIN' ? 'primary' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag></template></el-table-column>
          <el-table-column prop="recommendationCount" label="推荐记录" width="100" align="center" />
          <el-table-column prop="planCount" label="志愿方案" width="100" align="center" />
          <el-table-column prop="conversationCount" label="AI会话" width="90" align="center" />
          <el-table-column label="注册时间" min-width="170"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column>
          <el-table-column label="状态" width="95"><template #default="{ row }"><el-tag :type="row.enabled === false ? 'warning' : 'success'" effect="light">{{ row.enabled === false ? '已停用' : '正常' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openUserSettings(row)">管理</el-button></template></el-table-column>
        </el-table>
        <el-pagination v-model:current-page="userPage" :page-size="pageSize" :total="usersTotal" layout="total, prev, pager, next" @current-change="loadUsers" />
      </section>
    </template>

    <template v-else-if="section === 'universities'">
      <section class="admin-filter-bar admin-filter-bar--entity">
        <label><span>院校名称/省份</span><el-input v-model.trim="universityKeyword" clearable placeholder="请输入院校名称或省份" @keyup.enter="universityPage = 1" /></label>
        <label><span>院校层次</span><el-select v-model="universityTier" clearable placeholder="全部"><el-option v-for="tier in universityTiers" :key="tier" :label="tier" :value="tier" /></el-select></label>
        <div class="admin-filter-actions"><el-button type="primary" @click="universityPage = 1">查询</el-button><el-button @click="resetUniversityFilters">重置</el-button></div>
        <el-button class="admin-create-button" type="primary" @click="openRecordDialog()">新增院校</el-button>
      </section>
      <section class="admin-table-panel">
        <el-table v-loading="loading" :data="pagedUniversities" height="100%">
          <el-table-column prop="name" label="院校名称" min-width="200" />
          <el-table-column prop="province" label="省份" width="110" />
          <el-table-column prop="tier" label="院校层次" width="120" />
          <el-table-column label="985" width="80" align="center"><template #default="{ row }"><el-tag :type="row.is985 ? 'success' : 'info'" effect="light">{{ row.is985 ? '是' : '否' }}</el-tag></template></el-table-column>
          <el-table-column label="211" width="80" align="center"><template #default="{ row }"><el-tag :type="row.is211 ? 'success' : 'info'" effect="light">{{ row.is211 ? '是' : '否' }}</el-tag></template></el-table-column>
          <el-table-column label="双一流" width="90" align="center"><template #default="{ row }"><el-tag :type="row.isDoubleFirstClass ? 'success' : 'info'" effect="light">{{ row.isDoubleFirstClass ? '是' : '否' }}</el-tag></template></el-table-column>
          <el-table-column prop="tags" label="标签" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column>
        </el-table>
        <el-pagination v-model:current-page="universityPage" :page-size="pageSize" :total="filteredUniversities.length" layout="total, prev, pager, next" />
      </section>
    </template>

    <template v-else-if="section === 'majors'">
      <section class="admin-filter-bar admin-filter-bar--entity admin-filter-bar--major">
        <label><span>专业名称/类别</span><el-input v-model.trim="majorKeyword" clearable placeholder="请输入专业名称或类别" @keyup.enter="majorPage = 1" /></label>
        <label><span>专业类别</span><el-select v-model="majorCategory" clearable placeholder="全部"><el-option v-for="category in majorCategories" :key="category" :label="category" :value="category" /></el-select></label>
        <label><span>学位类型</span><el-select v-model="majorDegreeType" clearable placeholder="全部"><el-option v-for="degree in majorDegreeTypes" :key="degree" :label="degree" :value="degree" /></el-select></label>
        <div class="admin-filter-actions"><el-button type="primary" @click="majorPage = 1">查询</el-button><el-button @click="resetMajorFilters">重置</el-button></div>
        <el-button class="admin-create-button" type="primary" @click="openRecordDialog()">新增专业</el-button>
      </section>
      <section class="admin-table-panel">
        <el-table v-loading="loading" :data="pagedMajors" height="100%">
          <el-table-column prop="name" label="专业名称" min-width="190" />
          <el-table-column prop="category" label="专业类别" width="130" />
          <el-table-column prop="degreeType" label="学位类型" width="130" />
          <el-table-column prop="subjectRequirement" label="选科要求" min-width="160" show-overflow-tooltip />
          <el-table-column prop="tags" label="标签" min-width="150" show-overflow-tooltip />
          <el-table-column prop="description" label="专业说明" min-width="240" show-overflow-tooltip />
          <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column>
        </el-table>
        <el-pagination v-model:current-page="majorPage" :page-size="pageSize" :total="filteredMajors.length" layout="total, prev, pager, next" />
      </section>
    </template>

    <template v-else-if="section === 'cutoffs'">
      <section class="admin-filter-bar admin-filter-bar--records"><label><span>院校</span><el-select v-model="cutoffFilters.universityId" filterable clearable placeholder="请选择院校"><el-option v-for="item in universities" :key="item.id" :label="item.name" :value="item.id" /></el-select></label><label><span>年份</span><el-input v-model="cutoffFilters.admissionYear" clearable placeholder="请输入年份" /></label><label><span>省份</span><el-input v-model.trim="cutoffFilters.province" clearable placeholder="请输入省份" /></label><label><span>科类</span><el-select v-model="cutoffFilters.subjectType" clearable placeholder="全部"><el-option label="物理类" value="PHYSICS" /><el-option label="历史类" value="HISTORY" /></el-select></label><div class="admin-filter-actions"><el-button type="primary" @click="loadCutoffs">查询</el-button><el-button @click="resetCutoffFilters">重置</el-button></div><el-button class="admin-create-button" type="primary" @click="openRecordDialog()">新增录取线</el-button></section>
      <section class="admin-table-panel"><el-table v-loading="loading" :data="pagedCutoffs" height="100%"><el-table-column label="院校" min-width="220"><template #default="{ row }">{{ universityName(row.universityId) }}</template></el-table-column><el-table-column prop="admissionYear" label="年份" width="100" /><el-table-column prop="province" label="招生省份" width="130" /><el-table-column label="科类" width="120"><template #default="{ row }">{{ subjectLabel(row.subjectType) }}</template></el-table-column><el-table-column prop="cutoffScore" label="最低分" width="110" /><el-table-column prop="minRank" label="最低位次" width="130" /><el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="cutoffPage" :page-size="pageSize" :total="cutoffs.length" layout="total, prev, pager, next" /></section>
    </template>

    <template v-else-if="section === 'majorCutoffs'">
      <section class="admin-filter-bar admin-filter-bar--records admin-filter-bar--major-cutoff"><label><span>院校</span><el-select v-model="majorCutoffFilters.universityId" filterable clearable placeholder="请选择院校"><el-option v-for="item in universities" :key="item.id" :label="item.name" :value="item.id" /></el-select></label><label><span>专业关键词</span><el-input v-model.trim="majorCutoffFilters.majorKeyword" clearable placeholder="请输入专业关键词" /></label><label><span>年份</span><el-input v-model="majorCutoffFilters.admissionYear" clearable placeholder="请输入年份" /></label><label><span>省份</span><el-input v-model.trim="majorCutoffFilters.province" clearable placeholder="请输入省份" /></label><label><span>科类</span><el-select v-model="majorCutoffFilters.subjectType" clearable placeholder="全部"><el-option label="物理类" value="PHYSICS" /><el-option label="历史类" value="HISTORY" /></el-select></label><div class="admin-filter-actions"><el-button type="primary" @click="loadMajorCutoffs">查询</el-button><el-button @click="resetMajorCutoffFilters">重置</el-button></div><el-button class="admin-create-button" type="primary" @click="openRecordDialog()">新增专业录取线</el-button></section>
      <section class="admin-table-panel"><el-table v-loading="loading" :data="pagedMajorCutoffs" height="100%"><el-table-column label="院校" min-width="190"><template #default="{ row }">{{ universityName(row.universityId) }}</template></el-table-column><el-table-column prop="majorName" label="专业名称" min-width="190" /><el-table-column prop="admissionYear" label="年份" width="90" /><el-table-column prop="province" label="招生省份" width="110" /><el-table-column label="科类" width="110"><template #default="{ row }">{{ subjectLabel(row.subjectType) }}</template></el-table-column><el-table-column prop="cutoffScore" label="最低分" width="100" /><el-table-column prop="minRank" label="最低位次" width="120" /><el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="majorCutoffPage" :page-size="pageSize" :total="majorCutoffs.length" layout="total, prev, pager, next" /></section>
    </template>

    <template v-else-if="section === 'ai'">
      <section class="admin-ai-panel" v-loading="loading">
        <div class="admin-ai-panel__heading">
          <div>
            <span class="admin-ai-panel__eyebrow">运行时配置</span>
            <h2>AI 模型与接口</h2>
            <p>保存后立即作用于新的 AI 对话和解析请求，无需重启服务。</p>
          </div>
          <el-tag :type="aiConfig.apiKeyConfigured ? 'success' : 'warning'" effect="light">
            {{ aiConfig.apiKeyConfigured ? `密钥已配置 ${aiConfig.apiKeyMasked}` : "尚未配置密钥" }}
          </el-tag>
        </div>

        <el-form class="admin-ai-form" label-position="top" @submit.prevent="saveAiConfig">
          <div class="admin-ai-form__grid">
            <el-form-item label="供应商名称">
              <el-input v-model.trim="aiConfig.provider" placeholder="例如 DeepSeek、通义千问" />
              <p class="admin-ai-field-help">用于标记当前 AI 服务供应商并写入调用日志；接口仍需兼容 OpenAI 协议。</p>
            </el-form-item>
            <el-form-item label="模型名称">
              <el-input v-model.trim="aiConfig.model" placeholder="例如 deepseek-v4-flash" />
            </el-form-item>
            <el-form-item class="admin-ai-form__wide" label="OpenAI 兼容接口地址">
              <el-input v-model.trim="aiConfig.baseUrl" placeholder="https://api.example.com" />
            </el-form-item>
            <el-form-item class="admin-ai-form__wide" label="API Key">
              <el-input v-model="aiConfig.apiKey" type="password" show-password autocomplete="new-password" placeholder="留空则保留当前密钥" />
            </el-form-item>
          </div>
          <el-alert
            v-if="aiTestResult"
            class="admin-ai-test-result"
            :type="aiTestResult.available ? 'success' : 'warning'"
            :title="aiTestResult.message"
            :description="`${aiTestResult.provider} · ${aiTestResult.model} · ${aiTestResult.available ? '响应耗时' : '失败前耗时'} ${aiTestResult.latencyMillis} ms`"
            :closable="false"
            show-icon
          />
          <div class="admin-ai-form__footer">
            <div>
              <p>当前来源：{{ aiConfig.apiKeySource === 'database' ? '管理端数据库配置' : (aiConfig.apiKeySource === 'environment' ? '服务器环境变量' : '未配置') }}。页面和接口不会回显完整密钥。</p>
            </div>
            <div class="admin-ai-form__actions">
              <el-button :loading="aiTesting" :disabled="aiSaving" @click="testAiConfig">检测可用性</el-button>
              <el-button type="primary" :loading="aiSaving" :disabled="aiTesting" @click="saveAiConfig">保存 AI 配置</el-button>
            </div>
          </div>
        </el-form>
      </section>
    </template>
  </main>

  <el-dialog v-model="settingsVisible" title="用户设置" width="480px" destroy-on-close>
    <div v-if="selectedUser" class="admin-user-dialog">
      <dl><div><dt>用户名</dt><dd>{{ selectedUser.username }}</dd></div><div><dt>报考资料</dt><dd>{{ profileLabel(selectedUser) }}</dd></div><div><dt>使用概况</dt><dd>推荐 {{ selectedUser.recommendationCount }} · 志愿方案 {{ selectedUser.planCount }} · AI会话 {{ selectedUser.conversationCount }}</dd></div></dl>
      <label><span>角色</span><el-select v-model="settingsForm.role" :disabled="selectedUser.username === currentUsername"><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></label>
      <label><span>账号状态</span><el-switch v-model="settingsForm.enabled" :disabled="selectedUser.username === currentUsername" active-text="正常" inactive-text="已停用" /></label>
      <p v-if="selectedUser.username === currentUsername" class="admin-form-note">当前管理员不能停用或降级自己的账号。</p>
    </div>
    <template #footer><el-button @click="settingsVisible = false">取消</el-button><el-button type="primary" :loading="settingsSubmitting" @click="saveUserSettings">保存设置</el-button></template>
  </el-dialog>

  <el-dialog v-model="recordDialogVisible" :title="recordDialogTitle" width="620px" destroy-on-close>
    <div class="admin-record-form">
      <template v-if="section === 'universities'"><label><span>院校名称</span><el-input v-model.trim="recordForm.name" /></label><label><span>省份</span><el-input v-model.trim="recordForm.province" /></label><label><span>院校层次</span><el-input v-model.trim="recordForm.tier" /></label><label class="admin-record-form__wide"><span>院校标签</span><el-input v-model.trim="recordForm.tags" /></label><div class="admin-record-form__wide admin-checkboxes"><el-checkbox v-model="recordForm.is985">985</el-checkbox><el-checkbox v-model="recordForm.is211">211</el-checkbox><el-checkbox v-model="recordForm.isDoubleFirstClass">双一流</el-checkbox></div></template>
      <template v-else-if="section === 'majors'"><label><span>专业名称</span><el-input v-model.trim="recordForm.name" /></label><label><span>专业类别</span><el-input v-model.trim="recordForm.category" /></label><label><span>学位类型</span><el-input v-model.trim="recordForm.degreeType" /></label><label><span>选科要求</span><el-input v-model.trim="recordForm.subjectRequirement" /></label><label class="admin-record-form__wide"><span>标签</span><el-input v-model.trim="recordForm.tags" /></label><label class="admin-record-form__wide"><span>专业说明</span><el-input v-model.trim="recordForm.description" type="textarea" :rows="3" /></label></template>
      <template v-else><label><span>院校</span><el-select v-model="recordForm.universityId" filterable><el-option v-for="item in universities" :key="item.id" :label="item.name" :value="item.id" /></el-select></label><label v-if="section === 'majorCutoffs'"><span>专业名称</span><el-input v-model.trim="recordForm.majorName" /></label><label><span>年份</span><el-input-number v-model="recordForm.admissionYear" :min="2000" :max="2100" /></label><label><span>省份</span><el-input v-model.trim="recordForm.province" /></label><label><span>科类</span><el-select v-model="recordForm.subjectType"><el-option label="物理类" value="PHYSICS" /><el-option label="历史类" value="HISTORY" /></el-select></label><label><span>最低分</span><el-input-number v-model="recordForm.cutoffScore" :min="0" :max="750" /></label><label><span>最低位次</span><el-input-number v-model="recordForm.minRank" :min="1" /></label></template>
    </div>
    <template #footer><el-button @click="recordDialogVisible = false">取消</el-button><el-button type="primary" :loading="recordSubmitting" @click="saveRecord">保存</el-button></template>
  </el-dialog>
    </section>
  </div>
</template>

<style>
.app-layout {
  display: grid;
  grid-template-columns: 272px minmax(0, 1fr);
  min-height: 100vh;
  background: #fff;
}

.app-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-width: 0;
  padding: 28px 18px 0;
  overflow: hidden;
  border-right: 1px solid #e7edf5;
  background: linear-gradient(180deg, #f8faff 0%, #f4f7ff 46%, #eaf1ff 72%, #cfdeff 100%);
}

.app-brand {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 0 10px;
}

.app-brand-lockup {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: #172033;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.02em;
  white-space: nowrap;
}

.app-brand-lockup__mark,
.app-user__avatar,
.summary-panel__icon,
.university-card__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #2f6fed;
  background: #eaf1ff;
}

.app-brand-lockup__mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  color: #f06418;
  background: #fff0e5;
  font-size: 21px;
}

.app-brand-lockup__badge {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 7px;
  border: 1px solid #bfd2ff;
  border-radius: 6px;
  color: #2f6fed;
  background: rgba(234, 241, 255, 0.88);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0;
}

.app-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 34px;
}

.app-nav__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 52px;
  padding: 0 18px;
  border: 0;
  border-radius: 9px;
  color: #667085;
  background: transparent;
  font: inherit;
  font-size: 16px;
  text-align: left;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}

.app-nav__item .el-icon {
  font-size: 20px;
}

.app-nav__item:hover {
  color: #245edb;
  background: #edf3ff;
}

.app-nav__item.is-active {
  color: #245edb;
  background: #e7efff;
  font-weight: 600;
}

.app-sidebar__art {
  flex: 0 0 auto;
  width: calc(100% + 36px);
  aspect-ratio: 0.8;
  margin: auto -18px 0;
  overflow: hidden;
  pointer-events: none;
}

.app-sidebar__art img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center bottom;
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, 0.55) 18%, #000 34%);
  mask-image: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, 0.55) 18%, #000 34%);
}

.app-content {
  min-width: 0;
  background: #f8faff;
}

.app-header {
  display: flex;
  align-items: stretch;
  min-height: 76px;
  height: 76px;
  padding: 0;
  border-bottom: 1px solid #e8edf4;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
}

.app-header__brand-column {
  display: flex;
  align-items: center;
  width: 272px;
  min-width: 272px;
  padding: 0 28px;
  border-right: 1px solid #e7edf5;
}

.app-header__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  min-width: 0;
  padding: 0 28px;
}

.app-header__content h1 {
  margin: 0;
  color: #1e293b;
  font-size: 22px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.app-user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: #344054;
  font-size: 14px;
}

.app-user__meta {
  color: #98a2b3;
  white-space: nowrap;
}

.app-user__avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  font-size: 19px;
}

.app-user__divider {
  width: 1px;
  height: 16px;
  margin: 0 2px;
  background: #dfe5ed;
}

.app-route-view {
  min-width: 0;
}

.app-main {
  padding: 22px 28px 40px;
}

/* Admin workspace reuses the shared app shell and visual tokens. */
.admin-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: calc(100vh - 76px);
  min-height: 0;
  padding: 24px 28px 28px;
  overflow: hidden;
  background: #f8faff;
}

.admin-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  flex: 0 0 auto;
  min-height: 92px;
  padding: 0 28px;
  border: 1px solid #e4eaf2;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
}

.admin-overview > div {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.admin-overview__icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  color: #1f6feb;
  background: #edf4ff;
  font-size: 22px;
}

.admin-overview > div:not(:last-child)::after {
  position: absolute;
  top: 28px;
  right: 0;
  width: 1px;
  height: 36px;
  content: "";
  background: #dce5f1;
}

.admin-overview span {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #667085;
  font-size: 14px;
}

.admin-overview strong {
  color: #1f6feb;
  font-size: 26px;
  font-weight: 650;
}

.admin-ai-panel {
  max-width: 920px;
  padding: 30px;
  border: 1px solid #f0dfd2;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(114, 57, 18, 0.06);
}

.admin-ai-panel__heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; padding-bottom: 24px; border-bottom: 1px solid #f2e7df; }
.admin-ai-panel__heading h2 { margin: 5px 0 8px; color: #1f2937; font-size: 24px; }
.admin-ai-panel__heading p { margin: 0; color: #7a879a; line-height: 1.7; }
.admin-ai-panel__eyebrow { color: #e86612; font-size: 12px; font-weight: 700; letter-spacing: 0.12em; }
.admin-ai-form { margin-top: 26px; }
.admin-ai-form__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 20px; }
.admin-ai-form__wide { grid-column: 1 / -1; }
.admin-ai-field-help { margin: 6px 0 0; color: #8a94a3; font-size: 12px; line-height: 1.6; }
.admin-ai-test-result { margin-top: 8px; }
.admin-ai-form__footer { display: flex; align-items: flex-end; justify-content: space-between; gap: 28px; margin-top: 8px; padding-top: 22px; border-top: 1px solid #f2e7df; }
.admin-ai-form__footer p { margin: 6px 0 0; color: #8a94a3; font-size: 12px; }
.admin-ai-form__actions { display: flex; flex: 0 0 auto; gap: 10px; }

.app-layout--admin .app-sidebar { border-right-color: #f0dfd2; background: linear-gradient(180deg, #fffaf6 0%, #fff5ed 48%, #ffe8d5 100%); }
.app-layout--admin .app-content,
.app-layout--admin .admin-view { background: #fffaf6; }
.app-layout--admin .app-brand-lockup__badge { border-color: #ffc79e; color: #e86612; background: #fff1e6; }
.app-layout--admin .app-nav__item:hover { color: #e86612; background: #fff0e5; }
.app-layout--admin .app-nav__item.is-active { color: #d95c08; background: #ffe8d5; }
.app-layout--admin .app-user__avatar,
.app-layout--admin .admin-overview__icon { color: #e86612; background: #fff0e5; }
.app-layout--admin .admin-overview { border-color: #f0dfd2; background: linear-gradient(180deg, #fff 0%, #fffaf6 100%); }
.app-layout--admin .admin-overview strong { color: #e86612; }
.app-layout--admin .admin-filter-bar,
.app-layout--admin .admin-section-toolbar,
.app-layout--admin .admin-table-panel { border-color: #f0dfd2; }
.app-layout--admin .el-button--primary:not(.is-link):not(.is-text) { border-color: #ff7a1a; background: #ff7a1a; }
.app-layout--admin .el-button--primary:not(.is-link):not(.is-text):hover,
.app-layout--admin .el-button--primary:not(.is-link):not(.is-text):focus { border-color: #ed6d12; background: #ed6d12; }
.app-layout--admin .el-button.is-link.el-button--primary {
  border-color: transparent;
  color: #e86612;
  background: transparent;
}
.app-layout--admin .el-button.is-link.el-button--primary:hover,
.app-layout--admin .el-button.is-link.el-button--primary:focus { color: #d95c08; background: transparent; }
.app-layout--admin .el-input__wrapper.is-focus,
.app-layout--admin .el-select__wrapper.is-focused { box-shadow: 0 0 0 1px #ff7a1a inset !important; }

@media (max-width: 900px) {
  .admin-ai-form__grid { grid-template-columns: 1fr; }
  .admin-ai-form__wide { grid-column: auto; }
  .admin-ai-panel__heading,
  .admin-ai-form__footer { align-items: stretch; flex-direction: column; }
  .admin-ai-form__actions { justify-content: flex-end; }
}

.admin-filter-bar,
.admin-section-toolbar,
.admin-table-panel {
  border: 1px solid #e4eaf2;
  border-radius: 14px;
  background: #fff;
}

.admin-filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) minmax(180px, 1fr) minmax(180px, 1fr) auto;
  gap: 24px;
  align-items: end;
  flex: 0 0 auto;
  padding: 20px 24px;
}

.admin-filter-bar--records {
  grid-template-columns: repeat(4, minmax(130px, 1fr)) auto 140px;
}

.admin-filter-bar--entity {
  grid-template-columns: minmax(260px, 1.7fr) minmax(180px, 1fr) auto 140px;
}

.admin-filter-bar--major {
  grid-template-columns: minmax(240px, 1.5fr) repeat(2, minmax(160px, 1fr)) auto 140px;
}

.admin-filter-bar--major-cutoff {
  grid-template-columns: repeat(5, minmax(110px, 1fr)) auto 180px;
}

.admin-filter-bar label,
.admin-user-dialog label,
.admin-record-form label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  color: #344054;
  font-size: 14px;
  font-weight: 500;
}

.admin-filter-bar .el-select,
.admin-record-form .el-select,
.admin-record-form .el-input-number,
.admin-user-dialog .el-select {
  width: 100%;
}

.admin-filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-filter-actions .el-button--primary,
.admin-section-toolbar .el-button--primary,
.admin-create-button {
  min-width: 116px;
}

.admin-create-button {
  align-self: end;
}

.admin-section-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  flex: 0 0 auto;
  padding: 20px 24px;
}

.admin-section-toolbar .el-input {
  max-width: 460px;
}

.admin-table-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.admin-table-panel .el-table {
  flex: 1 1 auto;
  --el-table-border-color: #edf1f6;
  --el-table-header-bg-color: #f8faff;
  --el-table-row-hover-bg-color: #f7faff;
}

.admin-table-panel .el-table th.el-table__cell {
  height: 52px;
  color: #344054;
  font-weight: 600;
}

.admin-table-panel .el-table td.el-table__cell {
  height: 58px;
  color: #475467;
}

.admin-table-panel .el-pagination {
  justify-content: flex-end;
  flex: 0 0 auto;
  padding: 16px 20px;
  border-top: 1px solid #edf1f6;
}

.admin-user-dialog {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.admin-user-dialog dl {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 16px;
  border-radius: 12px;
  background: #f8faff;
}

.admin-user-dialog dl > div {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
}

.admin-user-dialog dt {
  color: #98a2b3;
}

.admin-user-dialog dd {
  margin: 0;
  color: #344054;
}

.admin-form-note {
  margin: -4px 0 0;
  color: #98a2b3;
  font-size: 13px;
}

.admin-record-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 20px;
}

.admin-record-form__wide {
  grid-column: 1 / -1;
}

.admin-checkboxes {
  display: flex;
  flex-direction: row;
  gap: 24px;
  padding-top: 4px;
}

/* Account for the persistent 272px sidebar on common 1280/1366px desktops. */
@media (max-width: 1400px) {
  .admin-view {
    padding: 20px;
  }

  .admin-filter-bar,
  .admin-filter-bar--records,
  .admin-filter-bar--entity,
  .admin-filter-bar--major,
  .admin-filter-bar--major-cutoff {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .admin-filter-actions {
    justify-content: flex-end;
  }

  .admin-create-button {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .admin-view {
    height: auto;
    min-height: calc(100vh - 62px);
    overflow: visible;
    padding: 14px;
  }

  .admin-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 12px;
  }

  .admin-overview > div {
    min-height: 64px;
  }

  .admin-overview > div::after {
    display: none;
  }

  .admin-filter-bar,
  .admin-filter-bar--records,
  .admin-filter-bar--entity,
  .admin-filter-bar--major,
  .admin-filter-bar--major-cutoff,
  .admin-record-form {
    grid-template-columns: 1fr;
  }

  .admin-filter-actions,
  .admin-record-form__wide {
    grid-column: auto;
  }

  .admin-table-panel {
    min-height: 520px;
  }
}


.app-user__logout { display:inline-flex; align-items:center; gap:6px; padding:6px 0; border:0; color:#667085; background:transparent; font:inherit; cursor:pointer; }
.app-user__logout:hover { color:#e86612; }
@media (max-width:900px) {
  .app-layout { grid-template-columns:220px minmax(0,1fr); }
  .app-sidebar { padding-right:12px; padding-left:12px; }
  .app-sidebar__art { width:calc(100% + 24px); margin-right:-12px; margin-left:-12px; }
}
</style>
