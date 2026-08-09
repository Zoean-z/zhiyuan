import { resolve } from "node:path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  server: {
    host: "0.0.0.0",
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: mode === "mock"
      ? resolve(__dirname, "dist-mock")
      : resolve(__dirname, "../src/main/resources/static"),
    emptyOutDir: true
  }
}));
