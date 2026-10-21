import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Las llamadas a /api se redirigen al backend Spring Boot
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true, // permite abrir la app desde el celular con la IP del computador
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
