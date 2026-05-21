import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import MotionApp from './MotionApp.tsx'
import './motion.css'

createRoot(document.getElementById('motion-root')!).render(
  <StrictMode>
    <MotionApp />
  </StrictMode>,
)
