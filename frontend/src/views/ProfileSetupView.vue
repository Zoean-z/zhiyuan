<script setup>
import { inject } from "vue";
import { DataLine, Location, Notebook, Reading, SetUp } from "@element-plus/icons-vue";
import journeyImage from "../assets/admission-journey.png";
import { SUBJECT_OPTIONS } from "../utils/recommendation";

const { completeProfile, error, loading, logout, profileForm, provinces } = inject("workspace");
</script>

<template>
  <div class="auth-page">
    <header class="auth-topbar">
      <div class="auth-topbar__sidebar">
        <div class="auth-topbar__brand">
          <span class="auth-logo"><el-icon><Reading /></el-icon></span>
          <span>智愿AI报考平台</span>
        </div>
      </div>
      <div class="auth-topbar__content">
        <h1>完善信息</h1>
      </div>
    </header>

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
          <h2>学生基础信息</h2>
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

            <el-form-item label="高考分数">
              <el-input v-model="profileForm.score" type="number" :prefix-icon="DataLine" placeholder="请输入分数">
                <template #suffix>分</template>
              </el-input>
              <p class="profile-field-hint">系统将结合近年录取数据进行智能分析</p>
            </el-form-item>

            <el-checkbox v-model="profileForm.confirmed" class="profile-confirm">我已确认信息填写无误</el-checkbox>

            <el-button type="primary" class="profile-submit" :loading="loading" @click="completeProfile">
              下一步，开始推荐
            </el-button>
            <div v-if="error" class="error auth-error">{{ error }}</div>
          </el-form>

          <el-button link type="primary" class="profile-back" @click="logout">返回登录</el-button>
        </div>
      </section>
    </main>
  </div>
</template>
