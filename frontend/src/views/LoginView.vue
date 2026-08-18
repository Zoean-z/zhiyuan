<script setup>
import { inject, onMounted, reactive, ref, watch } from "vue";
import { Lock, Reading, User, UserFilled } from "@element-plus/icons-vue";
import { useRoute } from "vue-router";
import journeyImage from "../assets/admission-journey.png";
import GkHeader from "../components/GkHeader.vue";

const {
  adminLogin, authMode, error, loading, login, loginForm, register
} = inject("workspace");
const route = useRoute();
const adminDialogVisible = ref(false);
const adminForm = reactive({ username: "", password: "" });
const sliderValue = ref(loginForm.sliderVerified ? 100 : 0);

function applyRouteMode() {
  authMode.value = route.query.mode === "register" ? "register" : "login";
}

function switchMode(mode) {
  authMode.value = mode;
  error.value = "";
  if (mode === "register") resetSlider();
}

async function submit() {
  return authMode.value === "register" ? register() : login();
}

function updateSlider() {
  loginForm.sliderVerified = sliderValue.value >= 100;
}

function resetSlider() {
  sliderValue.value = 0;
  loginForm.sliderVerified = false;
}

async function submitAdminLogin() {
  const success = await adminLogin(adminForm);
  if (success) adminDialogVisible.value = false;
}

function openAdminLogin() {
  error.value = "";
  adminDialogVisible.value = true;
}

onMounted(applyRouteMode);
watch(() => route.query.mode, applyRouteMode);
</script>

<template>
  <div class="auth-page auth-page--orange">
    <GkHeader />

    <main class="auth-stage">
      <section class="auth-scene">
        <div class="auth-visual auth-visual--orange">
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
            {{ authMode === "login" ? "欢迎回来，登录后继续规划升学方向" : "完成安全验证，创建你的志愿规划账号" }}
          </p>

          <div class="auth-mode-tabs" role="tablist" aria-label="账号操作">
            <button type="button" :class="{ 'is-active': authMode === 'login' }" @click="switchMode('login')">账号登录</button>
            <button type="button" :class="{ 'is-active': authMode === 'register' }" @click="switchMode('register')">注册账号</button>
          </div>

          <el-form class="auth-form" :model="loginForm" @keyup.enter="submit">
            <el-form-item>
              <el-input v-model.trim="loginForm.username" :prefix-icon="User" placeholder="请输入用户名" autocomplete="username" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" :prefix-icon="Lock" type="password" show-password placeholder="请输入密码" :autocomplete="authMode === 'login' ? 'current-password' : 'new-password'" />
            </el-form-item>

            <template v-if="authMode === 'register'">
              <el-form-item class="auth-slider-form-item">
                <div class="auth-slider" :class="{ 'is-verified': loginForm.sliderVerified }">
                  <div class="auth-slider__label">
                    <span>{{ loginForm.sliderVerified ? "验证成功" : "按住滑块拖到最右端" }}</span>
                    <strong>{{ loginForm.sliderVerified ? "✓" : `${sliderValue}%` }}</strong>
                  </div>
                  <input
                    v-model.number="sliderValue"
                    class="auth-slider__range"
                    type="range"
                    min="0"
                    max="100"
                    step="1"
                    aria-label="滑块验证"
                    :aria-valuetext="loginForm.sliderVerified ? '验证成功' : `${sliderValue}%`"
                    @input="updateSlider"
                    @change="updateSlider"
                  />
                </div>
              </el-form-item>
            </template>

            <el-button type="primary" class="auth-submit" :loading="loading" :disabled="authMode === 'register' && !loginForm.sliderVerified" @click="submit">
              {{ authMode === "login" ? "登录" : "注册" }}
            </el-button>
            <div v-if="error" class="error auth-error">{{ error }}</div>
          </el-form>

          <div class="auth-secondary-actions">
            <button type="button" class="auth-admin-entry" @click="openAdminLogin">
              <el-icon><UserFilled /></el-icon>
              管理员登录
            </button>
          </div>
        </div>
      </section>
    </main>

    <footer class="auth-footer">智愿AI报考平台 · 智能规划每一次选择</footer>

    <el-dialog v-model="adminDialogVisible" title="管理员登录" width="420px" class="admin-login-dialog" destroy-on-close>
      <p class="admin-login-dialog__hint">仅限平台管理员账号进入管理端</p>
      <el-form :model="adminForm" label-position="top" @keyup.enter="submitAdminLogin">
        <el-form-item label="管理员账号">
          <el-input v-model.trim="adminForm.username" :prefix-icon="User" autocomplete="username" placeholder="请输入管理员账号" />
        </el-form-item>
        <el-form-item label="管理员密码">
          <el-input v-model="adminForm.password" :prefix-icon="Lock" type="password" show-password autocomplete="current-password" placeholder="请输入管理员密码" />
        </el-form-item>
        <div v-if="error" class="error auth-error admin-login-dialog__error">{{ error }}</div>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitAdminLogin">进入管理端</el-button>
      </template>
    </el-dialog>
  </div>
</template>
