<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  school: { type: Object, default: null },
  majors: { type: Array, default: () => [] }
});

const emit = defineEmits(["update:modelValue", "add-selected"]);

const selectedMajorKeys = ref([]);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value)
});

const schoolTags = computed(() => {
  if (!Array.isArray(props.school?.schoolTags)) {
    return [];
  }
  return props.school.schoolTags.filter(Boolean);
});

const selectedMajors = computed(() => {
  const selectedKeySet = new Set(selectedMajorKeys.value);
  return (props.majors || []).filter((item) => selectedKeySet.has(buildMajorKey(item)));
});

function buildMajorKey(item) {
  return String(item?.majorName || "").trim().toLowerCase();
}

function isMajorSelected(item) {
  return selectedMajorKeys.value.includes(buildMajorKey(item));
}

function toggleMajor(item) {
  const key = buildMajorKey(item);
  if (!key) {
    return;
  }
  if (selectedMajorKeys.value.includes(key)) {
    selectedMajorKeys.value = selectedMajorKeys.value.filter((value) => value !== key);
    return;
  }
  selectedMajorKeys.value = [...selectedMajorKeys.value, key];
}

function resetSelection() {
  selectedMajorKeys.value = [];
}

function handleAddSelected() {
  emit("add-selected", selectedMajors.value);
}

watch(() => props.school?.universityId, resetSelection);
watch(() => props.modelValue, (value) => {
  if (!value) {
    resetSelection();
  }
});
</script>

<template>
  <el-drawer
    v-model="visible"
    title="学校详情"
    size="680px"
    destroy-on-close
    class="school-detail-drawer"
  >
    <div v-if="school" v-loading="loading" class="school-detail-drawer__body">
      <div class="school-detail-drawer__hero">
        <div class="school-detail-drawer__title-row">
          <div>
            <h3 class="school-detail-drawer__name">{{ school.universityName || "-" }}</h3>
            <div class="school-detail-drawer__province">{{ school.universityProvince || "-" }}</div>
          </div>
        </div>
        <div v-if="schoolTags.length" class="school-detail-drawer__tags">
          <el-tag v-for="tag in schoolTags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
        </div>
        <div v-if="school.introduction" class="school-detail-drawer__intro">{{ school.introduction }}</div>
      </div>

      <div class="school-detail-drawer__section">
        <div class="school-detail-drawer__section-head">
          <span>专业列表</span>
          <span class="school-detail-drawer__count">已选 {{ selectedMajorKeys.length }} 个专业</span>
        </div>

        <div v-if="majors.length" class="school-detail-drawer__major-list">
          <div v-for="major in majors" :key="buildMajorKey(major)" class="school-detail-drawer__major-item">
            <div class="school-detail-drawer__major-main">
              <div class="school-detail-drawer__major-name">{{ major.majorName || "-" }}</div>
              <div class="school-detail-drawer__major-meta">
                <span>最低分 {{ major.cutoffScore ?? "-" }}</span>
                <span>最低位次 {{ major.minRank ?? "-" }}</span>
              </div>
            </div>
            <el-checkbox :model-value="isMajorSelected(major)" @change="toggleMajor(major)" />
          </div>
        </div>
        <el-empty v-else :image-size="90" description="暂无专业录取数据" />
      </div>
    </div>

    <template #footer>
      <div class="school-detail-drawer__footer">
        <span class="school-detail-drawer__footer-text">已选 {{ selectedMajorKeys.length }} 个专业</span>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedMajorKeys.length" @click="handleAddSelected">
          加入方案
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
