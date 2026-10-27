import { useId } from 'react'
import { hojeIso } from '../lancamentos/formatar'
import { periodoDoAtalho, type Atalho } from './periodo'
import type { Periodo } from './types'

const ATALHOS: { valor: Atalho; rotulo: string }[] = [
  { valor: 'mes', rotulo: 'Este mês' },
  { valor: 'passado', rotulo: 'Mês passado' },
  { valor: 'ano', rotulo: 'Este ano' },
  { valor: 'outro', rotulo: 'Outro período' },
]

type Props = {
  atalho: Atalho
  periodo: Periodo
  onChange: (atalho: Atalho, periodo: Periodo) => void
  erro?: string | null
}

export function PeriodoSeletor({ atalho, periodo, onChange, erro }: Props) {
  const id = useId()
  return (
    <fieldset className="rl-periodo">
      <legend className="cf-label">Período</legend>
      <div className="cl-filter" role="group" aria-label="Atalhos de período">
        {ATALHOS.map((a) => (
          <button
            key={a.valor}
            type="button"
            className="cl-filter__option"
            aria-pressed={atalho === a.valor}
            onClick={() => onChange(a.valor, a.valor === 'outro' ? periodo : periodoDoAtalho(a.valor))}
          >
            {a.rotulo}
          </button>
        ))}
      </div>
      {atalho === 'outro' && (
        <div className="rl-periodo__datas">
          <label className="lc-periodo__campo" htmlFor={`${id}-de`}>
            <span>De</span>
            <input
              id={`${id}-de`}
              type="date"
              className="cf-input"
              value={periodo.de}
              max={periodo.ate || hojeIso()}
              onChange={(e) => onChange('outro', { ...periodo, de: e.target.value })}
            />
          </label>
          <label className="lc-periodo__campo" htmlFor={`${id}-ate`}>
            <span>Até</span>
            <input
              id={`${id}-ate`}
              type="date"
              className="cf-input"
              value={periodo.ate}
              min={periodo.de}
              onChange={(e) => onChange('outro', { ...periodo, ate: e.target.value })}
            />
          </label>
        </div>
      )}
      {erro ? <p className="cf-error">{erro}</p> : <p className="cf-hint">{textoPeriodo(periodo)}</p>}
    </fieldset>
  )
}

function textoPeriodo(p: Periodo) {
  if (!p.de || !p.ate) return ''
  const f = (iso: string) => iso.split('-').reverse().join('/')
  return `De ${f(p.de)} a ${f(p.ate)}`
}
