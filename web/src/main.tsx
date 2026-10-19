import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// Estilos base primeiro: os dos módulos (login, backoffice) vêm depois e podem sobrescrever
import './index.css'
import { App } from './app/App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
