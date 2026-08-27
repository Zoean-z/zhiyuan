import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import { refreshStoredAuthProfile } from "./utils/recommendation";
import "./styles.css";

async function bootstrap() {
  if (import.meta.env.VITE_MOCK === "true") {
    const { setupMockInterceptor, MOCK_USER } = await import("./utils/mock");
    setupMockInterceptor();
    if (!localStorage.getItem("zhiyuan_auth")) {
      const { token, ...user } = MOCK_USER;
      localStorage.setItem("zhiyuan_auth", JSON.stringify({ token, user }));
    }
    console.log("[Mock Mode] 已启用演示模式，使用模拟数据");
  }

  if (import.meta.env.VITE_MOCK !== "true") {
    try {
      await refreshStoredAuthProfile();
    } catch (error) {
      console.warn("[Auth] 无法刷新服务器考生档案，将等待用户重新登录或网络恢复。", error);
    }
  }

  createApp(App).use(ElementPlus).use(router).mount("#app");
}

bootstrap();
