import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import InsightApp from './InsightApp.tsx'
import './insight.css'

createRoot(document.getElementById('insight-root')!).render(
  <StrictMode>
    <InsightApp resultMode="search" />
  </StrictMode>,
)
