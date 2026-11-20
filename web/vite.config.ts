import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const learning = process.env.LEARNING_URL ?? 'http://localhost:8080'
const insights = process.env.INSIGHTS_URL ?? 'http://localhost:8081'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/insights': insights,
      '/api': learning,
    },
  },
  test: {
    environment: 'node',
  },
})
