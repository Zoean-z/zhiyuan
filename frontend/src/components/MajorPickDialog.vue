<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { majorDetailsOfSchool, normalizeSchoolLike, probOf, strategyOf } from "../utils/volunteerCore";

const props = defineProps({
  visible: { type: Boolean, default: false },
  item: { type: Object, default: null },
  strategy: { type: String, default: "safe" },
  userScore: { type: Number, default: null }
});
const emit = defineEmits(["update:visible", "placed"]);

const router = useRouter();
const dialogVisible = computed({
  get: () => props.visible,
  set: (v) => emit("update:visible", v)
});

const school = computed(() => normalizeSchoolLike(props.item || {}));
const majorList = computed(() => (props.item ? majorDetailsOfSchool(props.item) : []));
const probability = computed(() => {
  if (props.item?.admissionProbability != null) return Number(props.item.admissionProbability);
  if (props.userScore != null && props.item) return probOf(props.item, props.userScore);
  return null;
});
const tier = computed(() => {
  if (props.strategy === "rush") return { key: "rush", label: "冲" };
  if (props.strategy === "guarantee") return { key: "guard", label: "保" };
  return { key: "safe", label: "稳" };
});
const probText = computed(() => (probability.value == null ? "" : `${probability.value}%`));

const MAX_MAJORS = 6;
const selected = ref([]);
const adjust = ref(true);

watch(
  () => props.visible,
  (v) => {
    if (v) {
      selected.value = majorList.value.slice(0, 3).map((m) => m.name);
      adjust.value = true;
    }
  }
);

function toggleMajor(name) {
  const i = selected.value.indexOf(name);
  if (i >= 0) {
    selected.value.splice(i, 1);
    return;
  }
  if (selected.value.length >= MAX_MAJORS) {
    ElMessage.warning(`每个志愿组最多选择 ${MAX_MAJORS} 个专业`);
    return;
  }
  selected.value.push(name);
}

function majorMeta(name) {
  return majorList.value.find((m) => m.name === name) || {};
}

async function confirmPlace() {
  if (!selected.value.length) {
    ElMessage.warning("请至少选择 1 个专业");
    return;
  }
  const prob = probability.value ?? probOf(props.item, props.userScore);
  const strategyKey = props.strategy === "guarantee" ? "guarantee" : props.strategy;
  const { appendToCurrentSheet } = await import("../utils/volunteerCore");
  const result = appendToCurrentSheet(
    {
      schoolId: school.value.id,
      schoolName: school.value.name,
      majorNames: [...selected.value],
      adjust: adjust.value,
      prob
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
        <span>{{ school.province }}{{ school.type ? " · " + school.type : "" }} · 最多选 {{ MAX_MAJORS }} 个专业</span>
      </div>

      <div class="mnz-majorpick__list">
        <button
          v-for="m in majorList"
          :key="m.name"
          type="button"
          class="mnz-majorpick__major"
          :class="{ 'is-on': selected.includes(m.name) }"
          @click="toggleMajor(m.name)"
        >
          <em class="mnz-majorpick__check">{{ selected.includes(m.name) ? "✓" : "" }}</em>
          <span class="mnz-majorpick__mname">{{ m.name }}</span>
          <span class="mnz-majorpick__mmeta">{{ m.category }}<template v-if="m.salary"> · 月薪{{ m.salary }}</template></span>
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
        <button type="button" class="mnz-majorpick__go" @click="confirmPlace">
          选好了，投入志愿表（{{ selected.length }}/{{ MAX_MAJORS }}）
        </button>
      </div>
    </template>
  </el-dialog>
</template>
