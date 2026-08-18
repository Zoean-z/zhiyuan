<script setup>
import { computed, inject } from "vue";
import { useRoute } from "vue-router";
import { DataLine, Location, Notebook, SetUp } from "@element-plus/icons-vue";
import journeyImage from "../assets/admission-journey.png";
import GkHeader from "../components/GkHeader.vue";
import { ELECTIVE_SUBJECT_OPTIONS, SUBJECT_OPTIONS } from "../utils/recommendation";

const { completeProfile, error, loading, profileForm, provinces } = inject("workspace");
const route = useRoute();
const isEditing = computed(() => route.query.edit === "1");
</script>

<template>
  <div class="auth-page">
    <GkHeader />

    <main class="auth-stage auth-stage--profile">
      <section class="auth-scene auth-scene--profile">
        <div class="auth-visual">
          <div class="auth-visual__copy">
            <strong>完善报考信息</strong>
            <h1>填写基础信息，获取更精准的智能推荐</h1>
            <p>省份、首选科目、分数将作为推荐的重要依据</p>
          </div>
          <img :src="journeyImage" alt="校园升学路径插画" />
        </div>

        <div class="profile-form-panel">
          <h2>{{ isEditing ? "编辑高考信息" : "学生基础信息" }}</h2>
          <el-form class="profile-form" label-position="top" :model="profileForm">
            <el-form-item label="所在省份">
              <el-select v-model="profileForm.examProvince" placeholder="请选择省份">
                <template #prefix><el-icon><Location /></el-icon></template>
                <el-option v-for="province in provinces" :key="province" :label="province" :value="province" />
              </el-select>
            </el-form-item>

            <el-form-item label="首选科目">
              <el-radio-group v-model="profileForm.subjectType" class="profile-subjects">
                <el-radio-button v-for="option in SUBJECT_OPTIONS" :key="option.value" :value="option.value">
                  <el-icon><SetUp v-if="option.value === 'PHYSICS'" /><Notebook v-else /></el-icon>
                  {{ option.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="再选科目（选择 2 门）">
              <el-checkbox-group v-model="profileForm.electiveSubjects" :max="2" class="profile-electives">
                <el-checkbox-button v-for="option in ELECTIVE_SUBJECT_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </el-checkbox-button>
              </el-checkbox-group>
              <p class="profile-field-hint">已选择 {{ profileForm.electiveSubjects.length }}/2 门，将用于院校专业组选科过滤</p>
            </el-form-item>

            <el-form-item label="高考分数">
              <el-input v-model="profileForm.score" type="number" :prefix-icon="DataLine" placeholder="请输入分数">
                <template #suffix>分</template>
              </el-input>
              <p class="profile-field-hint">系统将结合近年录取数据进行智能分析</p>
            </el-form-item>

            <el-checkbox v-model="profileForm.confirmed" class="profile-confirm">我已确认信息填写无误</el-checkbox>

            <el-button type="primary" class="profile-submit" :loading="loading" @click="completeProfile">
              {{ isEditing ? "保存高考信息" : "下一步，开始推荐" }}
            </el-button>
            <div v-if="error" class="error auth-error">{{ error }}</div>
          </el-form>

        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.profile-electives { width: 100%; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.profile-electives :deep(.el-checkbox-button) { width: 100%; }
.profile-electives :deep(.el-checkbox-button__inner) { width: 100%; border: 1px solid #dfe4ec; border-radius: 8px !important; box-shadow: none !important; }
.profile-electives :deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) { border-color: #ff7a1a; background: #fff1e7; color: #e86612; }
@media (max-width: 520px) { .profile-electives { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
