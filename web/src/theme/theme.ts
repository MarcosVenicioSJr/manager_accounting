import type { CSSProperties } from 'react'

// Tema do tenant (escritório). O padrão é neutro, usado no login genérico.
export type TenantTheme = {
  tenantName: string
  logoUrl: string | null
  primaryColor: string
  secondaryColor: string
}

export const defaultTheme: TenantTheme = {
  tenantName: 'Manager Accounting',
  logoUrl: null,
  primaryColor: '#1A6BDC',
  secondaryColor: '#6b7280',
}

// ---------- Contraste ----------
// Cada escritório escolhe as próprias cores, então o app não pode confiar que elas tenham contraste.
// A partir da cor primária são derivadas versões escurecidas até atingir o contraste mínimo do WCAG AA.

const WHITE = '#ffffff'
const INK = '#0a1220'

type Rgb = [number, number, number]

function hexToRgb(hex: string): Rgb {
  const value = /^#[0-9a-f]{6}$/i.test(hex) ? hex.slice(1) : '1a6bdc'
  return [0, 2, 4].map((i) => parseInt(value.slice(i, i + 2), 16)) as Rgb
}

function rgbToHex([r, g, b]: Rgb): string {
  return `#${[r, g, b].map((c) => Math.round(c).toString(16).padStart(2, '0')).join('')}`
}

function luminance(hex: string): number {
  const [r, g, b] = hexToRgb(hex).map((c) => {
    const s = c / 255
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

export function contrast(a: string, b: string): number {
  const [l1, l2] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (l1 + 0.05) / (l2 + 0.05)
}

// Mistura a cor com outra (t = 0 mantém a original, t = 1 vira a outra).
function mix(hex: string, other: string, t: number): string {
  const a = hexToRgb(hex)
  const b = hexToRgb(other)
  return rgbToHex([0, 1, 2].map((i) => a[i] + (b[i] - a[i]) * t) as Rgb)
}

// Escurece a cor até o texto branco ter pelo menos o contraste pedido.
function darkenForWhiteText(hex: string, minContrast: number): string {
  for (let t = 0; t <= 1; t += 0.04) {
    const candidate = mix(hex, INK, t)
    if (contrast(candidate, WHITE) >= minContrast) return candidate
  }
  return INK
}

// Variáveis CSS do tema. Botões e links usam --color-primary-strong (contraste AA com branco);
// o menu lateral usa uma versão mais escura da primária, e a secundária marca o item ativo quando
// ela aparece bem sobre esse fundo (senão o destaque fica branco).
export function themeCssVars(theme: TenantTheme): CSSProperties {
  const primaryStrong = darkenForWhiteText(theme.primaryColor, 4.5)
  const sidebar = darkenForWhiteText(mix(theme.primaryColor, INK, 0.18), 7)
  const sidebarAccent = contrast(theme.secondaryColor, sidebar) >= 3 ? theme.secondaryColor : WHITE

  return {
    '--color-primary': theme.primaryColor,
    '--color-secondary': theme.secondaryColor,
    '--color-primary-strong': primaryStrong,
    '--color-sidebar': sidebar,
    '--color-sidebar-accent': sidebarAccent,
  } as CSSProperties
}
