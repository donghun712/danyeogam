import path from "node:path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "./src"),
    },
  },
  server: {
    port: 5173, // 백엔드 CORS_ALLOWED_ORIGINS 기본값(http://localhost:5173)과 일치시켜야 함
    strictPort: true,
    proxy: {
      // 백엔드 docs/frontend-session-api.md 권장: dev-server에서 /api를 백엔드로 프록시
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
