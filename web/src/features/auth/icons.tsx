import type { SVGProps } from 'react'

// Ícones da tela de acesso (traço de 1,5 a 1,8 px, iguais aos do visual aprovado).
type IconProps = SVGProps<SVGSVGElement>

function Icon({ children, ...props }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      width="22"
      height="22"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      {children}
    </svg>
  )
}

export function BarsLogo(props: IconProps) {
  return (
    <svg aria-hidden="true" width="44" height="48" viewBox="0 0 44 48" fill="none" {...props}>
      <rect x="0" y="22" width="12" height="26" rx="2" fill="#2F7BEA" />
      <rect x="16" y="6" width="12" height="42" rx="2" fill="#5B9BF2" />
      <rect x="32" y="14" width="12" height="34" rx="2" fill="#1A6BDC" />
    </svg>
  )
}

export function UserIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <circle cx="12" cy="8" r="3.6" />
      <path d="M4.8 19.5c.9-3.4 3.6-5.2 7.2-5.2s6.3 1.8 7.2 5.2" />
    </Icon>
  )
}

export function LockIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <rect x="5" y="10.5" width="14" height="9.5" rx="2" />
      <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" />
    </Icon>
  )
}

export function EyeIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
      <circle cx="12" cy="12" r="2.8" />
    </Icon>
  )
}

export function EyeOffIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M3 3l18 18" />
      <path d="M10.6 6a9.6 9.6 0 0 1 1.4-.1c6 0 9.5 6.1 9.5 6.1a16 16 0 0 1-3.2 3.9" />
      <path d="M6.5 7.6A15.7 15.7 0 0 0 2.5 12S6 18.5 12 18.5c1.5 0 2.8-.4 4-.9" />
      <path d="M9.9 9.9a2.8 2.8 0 0 0 4.2 3.7" />
    </Icon>
  )
}

export function EnterIcon(props: IconProps) {
  return (
    <Icon strokeWidth="1.8" {...props}>
      <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
      <path d="M4 12h11" />
      <path d="M11 8l4 4-4 4" />
    </Icon>
  )
}

export function SpinnerIcon(props: IconProps) {
  return (
    <Icon className="auth-spin" width="20" height="20" strokeWidth="2.5" {...props}>
      <path d="M12 3a9 9 0 1 0 9 9" />
    </Icon>
  )
}

export function AlertIcon(props: IconProps) {
  return (
    <Icon width="20" height="20" strokeWidth="1.75" {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7.5v5" />
      <path d="M12 16.2h.01" />
    </Icon>
  )
}

export function ShieldCheckIcon(props: IconProps) {
  return (
    <Icon width="24" height="24" strokeWidth="1.5" {...props}>
      <path d="M12 3l7.5 2.8v5.7c0 4.6-3.1 8.3-7.5 9.5-4.4-1.2-7.5-4.9-7.5-9.5V5.8L12 3Z" />
      <path d="M8.8 12.1l2.3 2.3 4.2-4.4" />
    </Icon>
  )
}

export function BoltIcon(props: IconProps) {
  return (
    <Icon width="24" height="24" strokeWidth="1.5" {...props}>
      <path d="M13 2.5L5 13.5h6l-1 8 8-11h-6l1-8Z" />
    </Icon>
  )
}

export function TrendIcon(props: IconProps) {
  return (
    <Icon width="24" height="24" strokeWidth="1.5" {...props}>
      <path d="M3 21h18" />
      <path d="M5 17v-4" />
      <path d="M10 17V9" />
      <path d="M15 17v-6" />
      <path d="M20 17V6" />
      <path d="M4 8.5l5-3 4 2.5 6-4.5" />
    </Icon>
  )
}

export function MailIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <rect x="3" y="5.5" width="18" height="13" rx="2" />
      <path d="M3.8 7l8.2 6 8.2-6" />
    </Icon>
  )
}

export function CheckCircleIcon(props: IconProps) {
  return (
    <Icon strokeWidth="1.8" {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M8.2 12.3l2.6 2.6 5-5.3" />
    </Icon>
  )
}

export function ArrowLeftIcon(props: IconProps) {
  return (
    <Icon width="18" height="18" strokeWidth="1.8" {...props}>
      <path d="M19 12H5" />
      <path d="M11 6l-6 6 6 6" />
    </Icon>
  )
}
