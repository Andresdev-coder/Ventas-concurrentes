import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Vite sirve el frontend en desarrollo y prepara el bundle optimizado para producción.
export default defineConfig({
  // Este plugin transforma JSX y habilita la integración de React con Vite.
  plugins: [react()],
});
