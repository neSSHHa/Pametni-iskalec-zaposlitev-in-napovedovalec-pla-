import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import BusinessApp from './BusinessApp.tsx'
import './business.css'

createRoot(document.getElementById('business-root')!).render(
  <StrictMode>
    <BusinessApp />
  </StrictMode>,
)
