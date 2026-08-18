import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
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

  createApp(App).use(ElementPlus).use(router).mount("#app");
}

bootstrap();
