<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { normalizeItem, readStoredAuth } from "../utils/recommendation";
import { appendToCurrentSheet } from "../utils/volunteerCore";

const props = defineProps({
  visible: { type: Boolean, default: false },
  item: { type: Object, default: null },
  strategy: { type: String, default: "safe" },
  province: { type: String, default: "" },
  subjectType: { type: String, default: "" }
});
const emit = defineEmits(["update:visible", "placed"]);

const router = useRouter();
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit("update:visible", value)
});

const model = computed(() => normalizeItem(props.item || {}, props.strategy));
const school = computed(() => ({
  id: model.value.universityId,
  name: model.value.universityName,
  province: model.value.universityProvince || ""
}));
const probability = computed(() => {
  const value = model.value.admissionProbability;
  return value == null || !Number.isFinite(Number(value)) ? null : Number(value);
});
const tier = computed(() => {
  if (props.strategy === "rush") return { key: "rush", label: "冲" };
  if (props.strategy === "guarantee") return { key: "guard", label: "保" };
  return { key: "safe", label: "稳" };
});
const probText = computed(() => (probability.value == null ? "" : `${probability.value}%`));

const MAX_MAJORS = 6;
const majorList = ref([]);
const majorLoading = ref(false);
const majorLoadError = ref("");
const selected = ref([]);
const adjust = ref(true);
let loadVersion = 0;

function normalizeMajor(row) {
  const name = String(row?.majorName || "").trim();
  if (!name) return null;
  return {
    name,
    cutoffScore: row.cutoffScore == null ? null : Number(row.cutoffScore),
    minRank: row.minRank == null ? null : Number(row.minRank)
  };
}

async function loadMajors() {
  const version = ++loadVersion;
  majorList.value = [];
  selected.value = [];
  adjust.value = true;
  majorLoadError.value = "";
  if (!props.visible) return;

  const universityId = school.value.id;
  const province = String(props.province || "").trim();
  const subjectType = String(props.subjectType || "").trim().toUpperCase();
  if (universityId == null || !province || !subjectType) {
    majorLoadError.value = "缺少本次推荐的省份或科类，暂时无法查询开设专业";
    return;
  }

  const token = readStoredAuth()?.token;
  if (!token) {
    majorLoadError.value = "登录状态已失效，请重新登录后查询";
    return;
  }

  majorLoading.value = true;
  try {
    const params = new URLSearchParams({ province, subjectType });
    const response = await fetch(`/api/recommendations/schools/${universityId}/majors?${params.toString()}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    if (version !== loadVersion) return;
    const unique = new Map();
    (Array.isArray(data?.majors) ? data.majors : []).forEach((row) => {
      const major = normalizeMajor(row);
      if (major && !unique.has(major.name)) unique.set(major.name, major);
    });
    majorList.value = [...unique.values()];
  } catch (error) {
    if (version !== loadVersion) return;
    console.error("[MajorPickDialog] 加载开设专业失败", error);
    majorLoadError.value = "开设专业加载失败，请稍后重试";
  } finally {
    if (version === loadVersion) majorLoading.value = false;
  }
}

watch(
  () => [props.visible, props.item, props.province, props.subjectType],
  () => loadMajors()
);

function toggleMajor(name) {
  const index = selected.value.indexOf(name);
  if (index >= 0) {
    selected.value.splice(index, 1);
    return;
  }
  if (selected.value.length >= MAX_MAJORS) {
    ElMessage.warning(`每个志愿组最多选择 ${MAX_MAJORS} 个专业`);
    return;
  }
  selected.value.push(name);
}

function confirmPlace() {
  if (!selected.value.length) {
    ElMessage.warning("请至少选择 1 个专业");
    return;
  }
  const strategyKey = props.strategy === "guarantee" ? "guarantee" : props.strategy;
  const result = appendToCurrentSheet(
    {
      schoolId: school.value.id,
      schoolName: school.value.name,
      majorNames: [...selected.value],
      adjust: adjust.value,
      prob: probability.value,
      minRank: model.value.minRank ?? null,
      schoolSource: "backend",
      probabilitySource: probability.value == null ? null : "backend",
      dataSource: "backend",
      majorSource: "backend"
    },
    strategyKey
  );
  if (!result.ok) {
    ElMessage.error(result.message);
    return;
  }
  emit("placed", result);
  ElMessage.success(`已投入志愿表第 ${result.position} 位（${result.segLabel}）`);
  dialogVisible.value = false;
}

function goSheet() {
  dialogVisible.value = false;
  router.push({ path: "/volunteer", query: { autostart: "1" } });
}
</script>

<template>
  <el-dialog v-model="dialogVisible" width="560px" class="mnz-majorpick" :close-on-click-modal="true" append-to-body>
    <template #header>
      <div class="mnz-majorpick__head">
        <h4>可填专业（{{ majorList.length }}）</h4>
        <span v-if="probability != null" class="mnz-majorpick__prob" :class="`is-${tier.key}`">
          {{ probText }} {{ tier.label }}
        </span>
      </div>
    </template>

    <div v-if="item" class="mnz-majorpick__body">
      <div class="mnz-majorpick__school">
        <strong>{{ school.name }}</strong>
        <span>{{ school.province }}{{ school.province ? " · " : "" }}最多选 {{ MAX_MAJORS }} 个专业</span>
      </div>

      <div v-if="majorLoading" class="mnz-majorpick__list">
        <el-skeleton :rows="4" animated />
      </div>
      <el-empty v-else-if="majorLoadError" :description="majorLoadError" :image-size="72" />
      <el-empty v-else-if="!majorList.length" description="当前省份和科类暂无专业录取数据" :image-size="72" />
      <div v-else class="mnz-majorpick__list">
        <button
          v-for="major in majorList"
          :key="major.name"
          type="button"
          class="mnz-majorpick__major"
          :class="{ 'is-on': selected.includes(major.name) }"
          @click="toggleMajor(major.name)"
        >
          <em class="mnz-majorpick__check">{{ selected.includes(major.name) ? "✓" : "" }}</em>
          <span class="mnz-majorpick__mname">{{ major.name }}</span>
          <span class="mnz-majorpick__mmeta">
            <template v-if="major.cutoffScore != null">最低分 {{ major.cutoffScore }}</template>
            <template v-if="major.cutoffScore != null && major.minRank != null"> · </template>
            <template v-if="major.minRank != null">最低位次 {{ major.minRank.toLocaleString("zh-CN") }}</template>
            <template v-if="major.cutoffScore == null && major.minRank == null">暂无录取数据</template>
          </span>
        </button>
      </div>

      <div class="mnz-majorpick__foot">
        <label class="mnz-majorpick__adjust">
          <input v-model="adjust" type="checkbox" />
          服从专业调剂
        </label>
        <span class="mnz-majorpick__hint">选好后投入志愿表，可在「志愿填报器」中继续调整顺序</span>
      </div>
    </div>

    <template #footer>
      <div class="mnz-majorpick__ops">
        <button type="button" class="mnz-majorpick__ghost" @click="goSheet">先看志愿表</button>
        <button type="button" class="mnz-majorpick__go" :disabled="majorLoading || !selected.length" @click="confirmPlace">
          选好了，投入志愿表（{{ selected.length }}/{{ MAX_MAJORS }}）
        </button>
      </div>
    </template>
  </el-dialog>
</template>
