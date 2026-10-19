import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { useBlocker } from 'react-router-dom'
import { AlertIcon } from '../../components/icons'
import { ConfirmDialog } from '../../components/ui/Dialog'
import { getErrorCode, getErrorMessage } from '../../services/errors'
import { documentoValido, limparDocumento, mascararDocumento } from './documento'
import { mascararTelefone } from './formatar'
import type { ClienteFormValores } from './clienteFormValores'
import { REGIMES, type ClienteDados, type RegimeTributario, type TipoPessoa } from './types'

type Campo = keyof ClienteFormValores
type Erros = Partial<Record<Campo, string>>

const MAX_OBSERVACOES = 1000
const DICA_CNPJ = 'Aceita o CNPJ com letras (formato novo da Receita).'
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function paraDados(v: ClienteFormValores): ClienteDados {
  const juridica = v.tipoPessoa === 'JURIDICA'
  const texto = (s: string) => s.trim() || null
  return {
    tipoPessoa: v.tipoPessoa,
    documento: limparDocumento(v.documento),
    nome: v.nome.trim(),
    nomeFantasia: juridica ? texto(v.nomeFantasia) : null,
    regimeTributario: juridica && v.regimeTributario ? v.regimeTributario : null,
    email: texto(v.email),
    telefone: v.telefone.replace(/\D/g, '') || null,
    observacoes: texto(v.observacoes),
  }
}

function validar(v: ClienteFormValores): Erros {
  const erros: Erros = {}
  const juridica = v.tipoPessoa === 'JURIDICA'
  const nomeDoc = juridica ? 'CNPJ' : 'CPF'

  if (!v.documento.trim()) erros.documento = `Informe o ${nomeDoc}.`
  else if (!documentoValido(v.documento, v.tipoPessoa)) erros.documento = `${nomeDoc} inválido. Confira os números.`

  if (!v.nome.trim()) erros.nome = juridica ? 'Informe a razão social.' : 'Informe o nome completo.'

  if (v.email.trim() && !EMAIL.test(v.email.trim())) erros.email = 'E-mail inválido.'

  const telefone = v.telefone.replace(/\D/g, '')
  if (telefone && (telefone.length < 10 || telefone.length > 11)) erros.telefone = 'Informe o DDD e o número.'

  if (v.observacoes.length > MAX_OBSERVACOES) erros.observacoes = `Máximo de ${MAX_OBSERVACOES} caracteres.`
  return erros
}

type ClienteFormProps = {
  valoresIniciais: ClienteFormValores
  submitLabel: string
  onSubmit: (dados: ClienteDados) => Promise<void>
  onCancelar?: () => void
  // Chamado quando outra pessoa salvou o cliente antes (409 EDICAO_SIMULTANEA)
  onRecarregar?: () => void
  // Conteúdo extra no topo do formulário (ex.: aviso de cliente inativo)
  aviso?: ReactNode
  // Na edição o botão só habilita depois de alguma alteração
  exigirAlteracao?: boolean
}

// Formulário de cliente, usado no cadastro e na edição. Os valores iniciais valem só na montagem: quem usa
// troca a `key` quando chega uma versão nova do cliente (depois de salvar ou de recarregar num conflito).
// Valida no navegador com as mesmas regras do backend,
// mostra o erro em cada campo e avisa antes de sair da página com alterações não salvas.
export function ClienteForm({
  valoresIniciais,
  submitLabel,
  onSubmit,
  onCancelar,
  onRecarregar,
  aviso,
  exigirAlteracao = false,
}: ClienteFormProps) {
  const [valores, setValores] = useState(valoresIniciais)
  const [tocados, setTocados] = useState<Partial<Record<Campo, boolean>>>({})
  const [tentouEnviar, setTentouEnviar] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erroServidor, setErroServidor] = useState<{ mensagem: string; recarregar: boolean } | null>(null)
  const [erroDocumentoServidor, setErroDocumentoServidor] = useState<string | null>(null)
  const alertaRef = useRef<HTMLDivElement>(null)

  const erros = useMemo(() => validar(valores), [valores])
  const alterado = useMemo(
    () => JSON.stringify(paraDados(valores)) !== JSON.stringify(paraDados(valoresIniciais)),
    [valores, valoresIniciais],
  )

  // Alterações não salvas: confirma antes de trocar de página dentro do app e antes de fechar a aba
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    alterado && !enviando && currentLocation.pathname !== nextLocation.pathname,
  )
  useEffect(() => {
    if (!alterado) return
    const onBeforeUnload = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [alterado])

  useEffect(() => {
    if (erroServidor) alertaRef.current?.focus()
  }, [erroServidor])

  const juridica = valores.tipoPessoa === 'JURIDICA'

  function erroDe(campo: Campo): string | undefined {
    if (campo === 'documento' && erroDocumentoServidor) return erroDocumentoServidor
    return tentouEnviar || tocados[campo] ? erros[campo] : undefined
  }

  // id do controle + ligação com a mensagem de erro/dica (o <Campo> usa os mesmos ids)
  function ariaDe(campo: Campo) {
    const erro = erroDe(campo)
    return {
      id: `cliente-${campo}`,
      'aria-invalid': Boolean(erro),
      'aria-describedby':
        erro || campo === 'observacoes' || (campo === 'documento' && juridica)
          ? `cliente-${campo}-desc`
          : undefined,
    }
  }

  function alterar<C extends Campo>(campo: C, valor: ClienteFormValores[C]) {
    setValores((atual) => ({ ...atual, [campo]: valor }))
    if (campo === 'documento') setErroDocumentoServidor(null)
  }

  function trocarTipo(tipo: TipoPessoa) {
    if (tipo === valores.tipoPessoa) return
    // O documento de um tipo não serve para o outro
    setValores((atual) => ({ ...atual, tipoPessoa: tipo, documento: '' }))
    setTocados((atual) => ({ ...atual, documento: false }))
    setErroDocumentoServidor(null)
  }

  function tocar(campo: Campo) {
    setTocados((atual) => ({ ...atual, [campo]: true }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTentouEnviar(true)
    setErroServidor(null)

    const primeiroErro = (Object.keys(erros) as Campo[])[0]
    if (primeiroErro) {
      document.getElementById(`cliente-${primeiroErro}`)?.focus()
      return
    }

    setEnviando(true)
    try {
      await onSubmit(paraDados(valores))
    } catch (error) {
      const codigo = getErrorCode(error)
      const mensagem = getErrorMessage(error, 'Não foi possível salvar o cliente. Tente novamente.')
      if (codigo === 'DOCUMENTO_DUPLICADO') {
        setErroDocumentoServidor(mensagem)
        document.getElementById('cliente-documento')?.focus()
      } else {
        setErroServidor({ mensagem, recarregar: codigo === 'EDICAO_SIMULTANEA' })
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="cf" onSubmit={handleSubmit} noValidate>
      {aviso}

      {erroServidor && (
        <div className="ui-alert ui-alert--error" role="alert" tabIndex={-1} ref={alertaRef}>
          <AlertIcon />
          <div className="ui-alert__content">
            {erroServidor.mensagem}
            {erroServidor.recarregar && onRecarregar && (
              <div className="ui-alert__actions">
                <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={onRecarregar}>
                  Recarregar cliente
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      <section className="panel cf-section" aria-labelledby="cf-ident">
        <h2 id="cf-ident" className="cf-section__title">
          Identificação
        </h2>

        <fieldset className="cf-tipo">
          <legend className="cf-label">Tipo de pessoa</legend>
          <div className="cf-segmented">
            {(
              [
                ['JURIDICA', 'Pessoa jurídica'],
                ['FISICA', 'Pessoa física'],
              ] as const
            ).map(([tipo, rotulo]) => (
              <label key={tipo} className="cf-segmented__option">
                <input
                  type="radio"
                  name="tipoPessoa"
                  value={tipo}
                  checked={valores.tipoPessoa === tipo}
                  onChange={() => trocarTipo(tipo)}
                />
                <span>{rotulo}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <div className="cf-grid">
          <Campo
            id="documento"
            rotulo={juridica ? 'CNPJ' : 'CPF'}
            obrigatorio
            erro={erroDe('documento')}
            dica={juridica ? DICA_CNPJ : undefined}
          >
            <input
              {...ariaDe('documento')}
              className="cf-input cf-input--mono"
              value={valores.documento}
              onChange={(e) => alterar('documento', mascararDocumento(e.target.value, valores.tipoPessoa))}
              onBlur={() => tocar('documento')}
              inputMode={juridica ? 'text' : 'numeric'}
              autoCapitalize="characters"
              autoComplete="off"
              placeholder={juridica ? '00.000.000/0000-00' : '000.000.000-00'}
            />
          </Campo>

          {juridica && (
            <Campo id="regimeTributario" rotulo="Regime tributário">
              <select
                {...ariaDe('regimeTributario')}
                className="cf-input"
                value={valores.regimeTributario}
                onChange={(e) => alterar('regimeTributario', e.target.value as RegimeTributario | '')}
              >
                <option value="">Não informado</option>
                {REGIMES.map((r) => (
                  <option key={r.valor} value={r.valor}>
                    {r.rotulo}
                  </option>
                ))}
              </select>
            </Campo>
          )}

          <Campo
            id="nome"
            rotulo={juridica ? 'Razão social' : 'Nome completo'}
            obrigatorio
            erro={erroDe('nome')}
            largo
          >
            <input
              {...ariaDe('nome')}
              className="cf-input"
              value={valores.nome}
              onChange={(e) => alterar('nome', e.target.value)}
              onBlur={() => tocar('nome')}
              maxLength={150}
              autoComplete="off"
            />
          </Campo>

          {juridica && (
            <Campo id="nomeFantasia" rotulo="Nome fantasia" largo>
              <input
                {...ariaDe('nomeFantasia')}
                className="cf-input"
                value={valores.nomeFantasia}
                onChange={(e) => alterar('nomeFantasia', e.target.value)}
                maxLength={150}
                autoComplete="off"
              />
            </Campo>
          )}
        </div>
      </section>

      <section className="panel cf-section" aria-labelledby="cf-contato">
        <h2 id="cf-contato" className="cf-section__title">
          Contato
        </h2>
        <div className="cf-grid">
          <Campo id="email" rotulo="E-mail" erro={erroDe('email')}>
            <input
              {...ariaDe('email')}
              type="email"
              className="cf-input"
              value={valores.email}
              onChange={(e) => alterar('email', e.target.value)}
              onBlur={() => tocar('email')}
              maxLength={150}
              autoComplete="off"
              placeholder="contato@empresa.com.br"
            />
          </Campo>
          <Campo id="telefone" rotulo="Telefone" erro={erroDe('telefone')}>
            <input
              {...ariaDe('telefone')}
              type="tel"
              className="cf-input"
              value={valores.telefone}
              onChange={(e) => alterar('telefone', mascararTelefone(e.target.value))}
              onBlur={() => tocar('telefone')}
              inputMode="tel"
              autoComplete="off"
              placeholder="(85) 99999-9999"
            />
          </Campo>
        </div>
      </section>

      <section className="panel cf-section" aria-labelledby="cf-obs">
        <h2 id="cf-obs" className="cf-section__title">
          Observações
        </h2>
        <Campo
          id="observacoes"
          rotulo="Anotações internas do escritório"
          erro={erroDe('observacoes')}
          dica={`${valores.observacoes.length}/${MAX_OBSERVACOES}`}
          largo
        >
          <textarea
            {...ariaDe('observacoes')}
            className="cf-input cf-textarea"
            value={valores.observacoes}
            onChange={(e) => alterar('observacoes', e.target.value)}
            onBlur={() => tocar('observacoes')}
            rows={4}
          />
        </Campo>
      </section>

      <div className="cf-actions">
        {onCancelar && (
          <button type="button" className="ui-btn ui-btn--secondary" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
        )}
        <button
          type="submit"
          className="ui-btn ui-btn--primary"
          disabled={enviando || (exigirAlteracao && !alterado)}
          aria-busy={enviando}
        >
          {enviando ? 'Salvando…' : submitLabel}
        </button>
      </div>

      <ConfirmDialog
        open={blocker.state === 'blocked'}
        title="Sair sem salvar?"
        confirmLabel="Sair sem salvar"
        tone="danger"
        onConfirm={() => blocker.proceed?.()}
        onClose={() => blocker.reset?.()}
      >
        <p>As alterações feitas neste cliente serão perdidas.</p>
      </ConfirmDialog>
    </form>
  )
}

type CampoProps = {
  id: Campo
  rotulo: string
  obrigatorio?: boolean
  erro?: string
  dica?: string
  largo?: boolean
  children: ReactNode
}

// Rótulo + controle + mensagem. O controle liga-se à mensagem pelo id `cliente-<campo>-desc` (ver ariaDe).
function Campo({ id, rotulo, obrigatorio, erro, dica, largo, children }: CampoProps) {
  const controle = `cliente-${id}`

  return (
    <div className={`cf-field${largo ? ' cf-field--wide' : ''}`} data-invalid={Boolean(erro)}>
      <label htmlFor={controle} className="cf-label">
        {rotulo}
        {obrigatorio && <span className="cf-required"> (obrigatório)</span>}
      </label>
      {children}
      {(erro || dica) && (
        <p id={`${controle}-desc`} className={erro ? 'cf-error' : 'cf-hint'}>
          {erro ?? dica}
        </p>
      )}
    </div>
  )
}
