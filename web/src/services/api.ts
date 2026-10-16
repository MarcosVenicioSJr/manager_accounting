import axios from 'axios'

// Cliente HTTP do backend. O token JWT será anexado aqui no módulo de autenticação.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
})
