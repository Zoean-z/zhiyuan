<script setup>
import { inject } from "vue";
import { Lock, User } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import BrandLockup from "../components/BrandLockup.vue";

const router = useRouter();
const { authMode, error, loading, login, loginForm, register } = inject("workspace");

const MODES = [
  { key: "login", label: "账号登录" },
  { key: "register", label: "注册账号" }
];

function switchMode(mode) {
  authMode.value = mode;
}

function submit() {
  return authMode.value === "register" ? register() : login();
}
</script>

<template>
  <div class="gk-auth">
    <header class="gk-auth__topbar">
      <div class="gk-auth__topbar-inner">
        <BrandLockup />
        <button class="gk-auth__back" type="button" @click="router.push({ name: 'home' })">返回首页</button>
      </div>
    </header>

    <main class="gk-auth__stage">
      <section class="gk-auth__card">
        <div class="gk-auth__visual">
          <div>
            <strong>AI 智能推荐</strong>
            <h2>科学规划<br />升学之路</h2>
            <p>数据驱动 · 专业洞察<br />成就每一份未来</p>
          </div>
          <ul class="gk-auth__points">
            <li>一分一段 · 位次精准换算</li>
            <li>冲稳保分档 · 45 个志愿位</li>
            <li>院校历年录取数据一键对比</li>
          </ul>
        </div>

        <div class="gk-auth__panel">
          <h1 class="gk-auth__title">{{ authMode === "login" ? "欢迎回来" : "创建账号" }}</h1>
          <p class="gk-auth__sub">
            {{ authMode === "login" ? "登录智愿AI，继续你的志愿规划" : "注册即可体验智能「冲稳保」推荐" }}
          </p>

          <div class="gk-auth__modes">
            <button
              v-for="mode in MODES"
              :key="mode.key"
              type="button"
              class="gk-auth__mode"
              :class="{ 'is-active': authMode === mode.key }"
              @click="switchMode(mode.key)"
            >
              {{ mode.label }}
            </button>
          </div>

          <el-form :model="loginForm" @keyup.enter="submit">
            <el-form-item>
              <el-input v-model.trim="loginForm.username" :prefix-icon="User" size="large" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model.trim="loginForm.password"
                :prefix-icon="Lock"
                type="password"
                size="large"
                show-password
                placeholder="请输入密码"
              />
            </el-form-item>

            <button class="gk-auth__submit" type="button" :disabled="loading" @click="submit">
              {{ loading ? "处理中…" : authMode === "login" ? "登 录" : "注册并继续" }}
            </button>
            <div v-if="error" class="gk-auth__error">{{ error }}</div>
          </el-form>

          <div class="gk-auth__switch">
            <span>{{ authMode === "login" ? "还没有账号？" : "已经有账号？" }}</span>
            <button type="button" @click="switchMode(authMode === 'login' ? 'register' : 'login')">
              {{ authMode === "login" ? "免费注册" : "返回登录" }}
            </button>
          </div>
        </div>
      </section>
    </main>

    <footer class="gk-auth__footer">© 2026 智愿AI报考平台 · 智能规划每一次选择</footer>
  </div>
</template>
