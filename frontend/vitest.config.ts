import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: [path.resolve(__dirname, "./src/vitest-setup.ts")],
    include: ["src/**/*.test.ts", "src/**/*.test.tsx"],
  },
});