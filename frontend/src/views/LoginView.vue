<script setup>
import { inject } from "vue";
import { Lock, Reading, User } from "@element-plus/icons-vue";
import journeyImage from "../assets/admission-journey.png";

const { authMode, error, loading, login, loginForm, register } = inject("workspace");

function submit() {
  return authMode.value === "register" ? register() : login();
}
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
        <h1>登录</h1>
      </div>
    </header>

    <main class="auth-stage">
      <section class="auth-scene">
        <div class="auth-visual">
          <div class="auth-visual__copy">
            <strong>AI 智能推荐</strong>
            <h1>科学规划升学之路</h1>
            <p>数据驱动 · 专业洞察 · 成就每一份未来</p>
          </div>
          <img :src="journeyImage" alt="校园升学路径插画" />
        </div>

        <div class="auth-form-panel">
          <div class="auth-form-panel__brand">
            <span class="auth-logo auth-logo--large"><el-icon><Reading /></el-icon></span>
            <h2>智愿AI报考平台</h2>
          </div>
          <p class="auth-form-panel__subtitle">
            {{ authMode === "login" ? "欢迎回来，登录后继续规划升学方向" : "创建账号，开始你的志愿规划" }}
          </p>

          <div class="auth-mode-title">{{ authMode === "login" ? "账号登录" : "注册账号" }}</div>

          <el-form class="auth-form" :model="loginForm" @keyup.enter="submit">
            <el-form-item>
              <el-input v-model.trim="loginForm.username" :prefix-icon="User" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" :prefix-icon="Lock" type="password" show-password placeholder="请输入密码" />
            </el-form-item>

            <el-button type="primary" class="auth-submit" :loading="loading" @click="submit">
              {{ authMode === "login" ? "登录" : "注册并继续" }}
            </el-button>
            <div v-if="error" class="error auth-error">{{ error }}</div>
          </el-form>

          <div class="auth-account-switch">
            <span>{{ authMode === "login" ? "还没有账号？" : "已经有账号？" }}</span>
            <el-button link type="primary" @click="authMode = authMode === 'login' ? 'register' : 'login'">
              {{ authMode === "login" ? "注册账号" : "返回登录" }}
            </el-button>
          </div>
        </div>
      </section>
    </main>

    <footer class="auth-footer">智愿AI报考平台 · 智能规划每一次选择</footer>
  </div>
</template>
