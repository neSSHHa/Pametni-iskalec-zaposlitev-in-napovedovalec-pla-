import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        business: resolve(__dirname, 'business.html'),
        insight: resolve(__dirname, 'insight.html'),
        insightPrompt: resolve(__dirname, 'insight-prompt.html'),
        insightCv: resolve(__dirname, 'insight-cv.html'),
        motion: resolve(__dirname, 'motion.html'),
        motionPrompt: resolve(__dirname, 'motion-prompt.html'),
        motionCv: resolve(__dirname, 'motion-cv.html'),
      },
    },
  },
})
