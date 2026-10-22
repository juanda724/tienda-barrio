import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Las llamadas a /api se redirigen al backend Spring Boot
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // También escucha en la red local, para abrir la app desde el celular del dueño en el mismo WiFi (SWR-10)
    host: true,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
