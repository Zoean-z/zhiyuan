<script setup>
import { inject } from "vue";
import { DataLine, Location, Notebook, SetUp } from "@element-plus/icons-vue";
import BrandLockup from "../components/BrandLockup.vue";
import { SUBJECT_OPTIONS } from "../utils/recommendation";

const { completeProfile, error, loading, logout, profileForm, provinces } = inject("workspace");
</script>

<template>
  <div class="gk-auth">
    <header class="gk-auth__topbar">
      <div class="gk-auth__topbar-inner">
        <BrandLockup />
        <button class="gk-auth__back" type="button" @click="logout">退出登录</button>
      </div>
    </header>

    <main class="gk-auth__stage">
      <section class="gk-auth__card gk-auth__card--profile">
        <div class="gk-auth__panel gk-auth__panel--profile">
          <h1 class="gk-auth__title">完善报考信息</h1>
          <p class="gk-auth__sub">省份、首选科目与分数将作为智能推荐的重要依据</p>

          <el-form label-position="top" :model="profileForm">
            <el-form-item label="所在省份">
              <el-select v-model="profileForm.examProvince" size="large" placeholder="请选择省份">
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

            <el-form-item label="高考分数">
              <el-input v-model="profileForm.score" size="large" type="number" :prefix-icon="DataLine" placeholder="请输入分数">
                <template #suffix>分</template>
              </el-input>
              <p class="gk-auth__hint">系统将结合近年录取数据进行智能分析</p>
            </el-form-item>

            <el-checkbox v-model="profileForm.confirmed" class="gk-auth__confirm">我已确认信息填写无误</el-checkbox>

            <button class="gk-auth__submit" type="button" :disabled="loading" @click="completeProfile">
              {{ loading ? "处理中…" : "下一步，开始推荐" }}
            </button>
            <div v-if="error" class="gk-auth__error">{{ error }}</div>
          </el-form>

          <div class="gk-auth__backline">
            <button type="button" @click="logout">返回登录</button>
          </div>
        </div>
      </section>
    </main>

    <footer class="gk-auth__footer">© 2026 智愿AI报考平台 · 智能规划每一次选择</footer>
  </div>
</template>
