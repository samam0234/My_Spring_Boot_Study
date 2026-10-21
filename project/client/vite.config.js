import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const api = {
  target: "http://localhost:8082",
  changeOrigin: true,
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/hello": api,
      "/auth": api,
      "/posts": api,
      "/members": api,
      "/messages": api,
      "/weather": api,
      "/stocks": api,
      "/watchlist": api,
      "/files": api,
      "/storage": api,
      "/time": api,
      "/notifications": api,
    },
  },
});
