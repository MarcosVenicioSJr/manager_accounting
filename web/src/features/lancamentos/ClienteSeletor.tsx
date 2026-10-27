import { useEffect, useId, useRef, useState } from 'react'
import { CloseIcon, SearchIcon } from '../../components/icons'
import { listarClientes } from '../clientes/clienteService'
import { formatarDocumento } from '../clientes/documento'
import type { ClienteListaItem } from '../clientes/types'

export type ClienteEscolhido = { id: number; nome: string }

type Props = {
  id: string
  valor: ClienteEscolhido | null
  onChange: (cliente: ClienteEscolhido | null) => void
  invalido?: boolean
  descritoPor?: string
}

const ESPERA_MS = 250

// Campo de busca de cliente (combobox): digita parte do nome, CPF/CNPJ ou e-mail e escolhe na lista.
// Só aparecem clientes ativos, porque cliente inativo não recebe lançamento.
export function ClienteSeletor({ id, valor, onChange, invalido, descritoPor }: Props) {
  const [texto, setTexto] = useState('')
  const [aberto, setAberto] = useState(false)
  const [opcoes, setOpcoes] = useState<ClienteListaItem[] | null>(null)
  const [ativo, setAtivo] = useState(0)
  const [erro, setErro] = useState(false)
  const listaId = useId()
  const caixaRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!aberto) return
    const controle = new AbortController()
    const timer = window.setTimeout(() => {
      listarClientes({ busca: texto.trim(), status: 'ATIVOS', pagina: 0, tamanho: 8 }, controle.signal)
        .then((pagina) => {
          setOpcoes(pagina.itens)
          setAtivo(0)
          setErro(false)
        })
        .catch(() => {
          if (!controle.signal.aborted) setErro(true)
        })
    }, ESPERA_MS)
    return () => {
      window.clearTimeout(timer)
      controle.abort()
    }
  }, [texto, aberto])

  useEffect(() => {
    if (!aberto) return
    const fora = (event: MouseEvent) => {
      if (!caixaRef.current?.contains(event.target as Node)) setAberto(false)
    }
    document.addEventListener('mousedown', fora)
    return () => document.removeEventListener('mousedown', fora)
  }, [aberto])

  function escolher(c: ClienteListaItem) {
    onChange({ id: c.id, nome: c.nome })
    setTexto('')
    setAberto(false)
  }

  if (valor) {
    return (
      <div className="lc-cliente-escolhido" data-invalid={invalido || undefined}>
        <span className="lc-cliente-escolhido__nome">{valor.nome}</span>
        <button
          type="button"
          className="ui-icon-btn"
          onClick={() => onChange(null)}
          aria-label={`Trocar o cliente (${valor.nome})`}
        >
          <CloseIcon width={16} height={16} />
        </button>
      </div>
    )
  }

  const itens = opcoes ?? []
  return (
    <div className="lc-combo" ref={caixaRef}>
      <SearchIcon className="lc-combo__icon" width={18} height={18} />
      <input
        id={id}
        className="cf-input lc-combo__input"
        role="combobox"
        aria-expanded={aberto}
        aria-controls={listaId}
        aria-autocomplete="list"
        aria-activedescendant={aberto && itens[ativo] ? `${listaId}-${itens[ativo].id}` : undefined}
        aria-invalid={invalido || undefined}
        aria-describedby={descritoPor}
        placeholder="Buscar por nome, CPF/CNPJ ou e-mail"
        autoComplete="off"
        value={texto}
        onFocus={() => setAberto(true)}
        onChange={(e) => {
          setTexto(e.target.value)
          setAberto(true)
        }}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown') {
            e.preventDefault()
            setAberto(true)
            setAtivo((i) => Math.min(i + 1, Math.max(itens.length - 1, 0)))
          } else if (e.key === 'ArrowUp') {
            e.preventDefault()
            setAtivo((i) => Math.max(i - 1, 0))
          } else if (e.key === 'Enter' && aberto && itens[ativo]) {
            e.preventDefault()
            escolher(itens[ativo])
          } else if (e.key === 'Escape' && aberto) {
            // Fecha só a lista, não o diálogo
            e.preventDefault()
            e.stopPropagation()
            setAberto(false)
          }
        }}
      />
      {aberto && (
        <ul className="lc-combo__lista" role="listbox" id={listaId} aria-label="Clientes ativos">
          {erro ? (
            <li className="lc-combo__msg">Não foi possível buscar os clientes.</li>
          ) : opcoes === null ? (
            <li className="lc-combo__msg">Buscando…</li>
          ) : itens.length === 0 ? (
            <li className="lc-combo__msg">Nenhum cliente ativo encontrado.</li>
          ) : (
            itens.map((c, i) => (
              <li
                key={c.id}
                id={`${listaId}-${c.id}`}
                role="option"
                aria-selected={i === ativo}
                className="lc-combo__opcao"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => escolher(c)}
                onMouseEnter={() => setAtivo(i)}
              >
                <span className="lc-combo__nome">{c.nome}</span>
                <span className="lc-combo__doc cl-mono">{formatarDocumento(c.documento, c.tipoPessoa)}</span>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  )
}
