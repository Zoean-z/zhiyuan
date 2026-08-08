<script setup>
import { ElMessage } from "element-plus";
import { CircleCloseFilled, Lock, User, UserFilled } from "@element-plus/icons-vue";
import { computed, inject, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";

defineOptions({ name: "AdminView" });

const route = useRoute();
const workspace = inject("workspace");
const section = computed(() => String(route.query.section || "users"));
const token = computed(() => workspace?.auth?.value?.token || "");
const currentUsername = computed(() => workspace?.auth?.value?.user?.username || "");
const loading = ref(false);

const overview = reactive({ totalCount: 0, userCount: 0, adminCount: 0, disabledCount: 0 });
const users = ref([]);
const userFilters = reactive({ keyword: "", role: "", enabled: "" });
const userPage = ref(1);
const pageSize = 10;
const pagedUsers = computed(() => users.value.slice((userPage.value - 1) * pageSize, userPage.value * pageSize));

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
    const [list, summary] = await Promise.all([
      api(`/api/admin/users${params.size ? `?${params}` : ""}`),
      api("/api/admin/users/overview")
    ]);
    users.value = Array.isArray(list) ? list : [];
    Object.assign(overview, summary || {});
    userPage.value = 1;
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}

function resetUserFilters() {
  Object.assign(userFilters, { keyword: "", role: "", enabled: "" });
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
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}

watch(section, loadSection);
onMounted(loadSection);
</script>

<template>
  <main class="admin-view">
    <template v-if="section === 'users'">
      <section class="admin-overview" aria-label="用户概览">
        <div><el-icon class="admin-overview__icon"><User /></el-icon><span>用户总数<strong>{{ overview.totalCount || 0 }}</strong></span></div>
        <div><el-icon class="admin-overview__icon"><UserFilled /></el-icon><span>普通用户<strong>{{ overview.userCount || 0 }}</strong></span></div>
        <div><el-icon class="admin-overview__icon"><Lock /></el-icon><span>管理员<strong>{{ overview.adminCount || 0 }}</strong></span></div>
        <div><el-icon class="admin-overview__icon"><CircleCloseFilled /></el-icon><span>已停用<strong>{{ overview.disabledCount || 0 }}</strong></span></div>
      </section>

      <section class="admin-filter-bar">
        <label><span>用户名</span><el-input v-model.trim="userFilters.keyword" clearable placeholder="请输入用户名" @keyup.enter="loadUsers" /></label>
        <label><span>角色</span><el-select v-model="userFilters.role"><el-option label="全部" value="" /><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></label>
        <label><span>账号状态</span><el-select v-model="userFilters.enabled"><el-option label="全部" value="" /><el-option label="正常" value="true" /><el-option label="已停用" value="false" /></el-select></label>
        <div class="admin-filter-actions"><el-button type="primary" @click="loadUsers">查询</el-button><el-button @click="resetUserFilters">重置</el-button></div>
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
        <el-pagination v-model:current-page="userPage" :page-size="pageSize" :total="users.length" layout="total, prev, pager, next" />
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

    <template v-else>
      <section class="admin-filter-bar admin-filter-bar--records admin-filter-bar--major-cutoff"><label><span>院校</span><el-select v-model="majorCutoffFilters.universityId" filterable clearable placeholder="请选择院校"><el-option v-for="item in universities" :key="item.id" :label="item.name" :value="item.id" /></el-select></label><label><span>专业关键词</span><el-input v-model.trim="majorCutoffFilters.majorKeyword" clearable placeholder="请输入专业关键词" /></label><label><span>年份</span><el-input v-model="majorCutoffFilters.admissionYear" clearable placeholder="请输入年份" /></label><label><span>省份</span><el-input v-model.trim="majorCutoffFilters.province" clearable placeholder="请输入省份" /></label><label><span>科类</span><el-select v-model="majorCutoffFilters.subjectType" clearable placeholder="全部"><el-option label="物理类" value="PHYSICS" /><el-option label="历史类" value="HISTORY" /></el-select></label><div class="admin-filter-actions"><el-button type="primary" @click="loadMajorCutoffs">查询</el-button><el-button @click="resetMajorCutoffFilters">重置</el-button></div><el-button class="admin-create-button" type="primary" @click="openRecordDialog()">新增专业录取线</el-button></section>
      <section class="admin-table-panel"><el-table v-loading="loading" :data="pagedMajorCutoffs" height="100%"><el-table-column label="院校" min-width="190"><template #default="{ row }">{{ universityName(row.universityId) }}</template></el-table-column><el-table-column prop="majorName" label="专业名称" min-width="190" /><el-table-column prop="admissionYear" label="年份" width="90" /><el-table-column prop="province" label="招生省份" width="110" /><el-table-column label="科类" width="110"><template #default="{ row }">{{ subjectLabel(row.subjectType) }}</template></el-table-column><el-table-column prop="cutoffScore" label="最低分" width="100" /><el-table-column prop="minRank" label="最低位次" width="120" /><el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openRecordDialog(row)">编辑</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="majorCutoffPage" :page-size="pageSize" :total="majorCutoffs.length" layout="total, prev, pager, next" /></section>
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
</template>
