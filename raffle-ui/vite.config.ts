import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // 只要以 /api 开头的请求，都转发到 8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 不要写 rewrite，因为后端接口本身就是以 /api 开头的
      }
    }
  }
})
