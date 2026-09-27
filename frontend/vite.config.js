import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Las llamadas a /api se redirigen al backend Spring Boot
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
