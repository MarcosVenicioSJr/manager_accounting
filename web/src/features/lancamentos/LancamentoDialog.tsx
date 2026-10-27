import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { AlertIcon, PaperclipIcon, TrashIcon, UploadIcon } from '../../components/icons'
import { Dialog } from '../../components/ui/Dialog'
import { getErrorCode, getErrorMessage } from '../../services/errors'
import { ClienteSeletor, type ClienteEscolhido } from './ClienteSeletor'
import { formatarTamanho, hojeIso, mascararValor, valorParaApi, valorParaCampo } from './formatar'
import {
  atualizarLancamento,
  criarLancamento,
  enviarComprovante,
  listarCategorias,
  removerComprovante,
} from './lancamentoService'
import { TIPOS, type Categoria, type Lancamento, type TipoLancamento } from './types'

const MAX_DESCRICAO = 200
const MAX_ARQUIVO = 10 * 1024 * 1024
const DATA_MINIMA = '2000-01-01'
const ACEITOS = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp']
const EXTENSOES = /\.(pdf|jpe?g|png|webp)$/i

type Valores = {
  cliente: ClienteEscolhido | null
  tipo: TipoLancamento
  data: string
  valor: string
  descricao: string
  categoria: string
}

type Campo = keyof Valores | 'arquivo'
type Erros = Partial<Record<Campo, string>>

function validar(v: Valores, hoje: string): Erros {
  const erros: Erros = {}
  if (!v.cliente) erros.cliente = 'Escolha o cliente.'
  if (!v.data) erros.data = 'Informe a data.'
  else if (v.data > hoje) erros.data = 'A data não pode ser no futuro.'
  else if (v.data < DATA_MINIMA) erros.data = 'Data inválida. Confira o ano.'
  const valor = Number(valorParaApi(v.valor))
  if (!v.valor || !(valor > 0)) erros.valor = 'Informe um valor maior que zero.'
  if (!v.descricao.trim()) erros.descricao = 'Informe a descrição.'
  else if (v.descricao.trim().length > MAX_DESCRICAO) erros.descricao = `Máximo de ${MAX_DESCRICAO} caracteres.`
  if (!v.categoria) erros.categoria = 'Escolha a categoria.'
  return erros
}

function validarArquivo(arquivo: File): string | null {
  if (!ACEITOS.includes(arquivo.type) && !EXTENSOES.test(arquivo.name)) {
    return 'Formato não aceito. Envie um PDF ou uma imagem JPG, PNG ou WEBP.'
  }
  if (arquivo.size > MAX_ARQUIVO) return 'Arquivo muito grande. O limite é de 10 MB.'
  if (arquivo.size === 0) return 'O arquivo está vazio.'
  return null
}

type Props = {
  open: boolean
  // Edição; null para um lançamento novo
  lancamento: Lancamento | null
  // Cliente já definido (aba do cliente): o campo de cliente não aparece
  clienteFixo?: ClienteEscolhido
  // Página geral filtrada por um cliente: o formulário novo já vem com ele (dá para trocar)
  clienteInicial?: ClienteEscolhido
  onClose: () => void
  onSalvo: (lancamento: Lancamento, mensagem: string) => void
  // Outra pessoa alterou o lançamento antes (409): fecha e recarrega a lista
  onConflito: () => void
  onAviso: (mensagem: string) => void
}

// Cadastro e edição de lançamento num diálogo. O comprovante vai depois de salvar os dados (é outra chamada);
// se só o envio do arquivo falhar, o lançamento continua salvo e a pessoa é avisada.
export function LancamentoDialog(props: Props) {
  // O formulário recomeça do zero a cada abertura
  if (!props.open) return null
  return <Formulario key={props.lancamento?.id ?? 'novo'} {...props} />
}

function Formulario({ lancamento, clienteFixo, clienteInicial, onClose, onSalvo, onConflito, onAviso }: Props) {
  const idBase = useId()
  const hoje = hojeIso()
  const [valores, setValores] = useState<Valores>(() => ({
    cliente:
      clienteFixo ?? (lancamento ? { id: lancamento.clienteId, nome: lancamento.clienteNome } : (clienteInicial ?? null)),
    tipo: lancamento?.tipo ?? 'DESPESA',
    data: lancamento?.data ?? hoje,
    valor: lancamento ? valorParaCampo(lancamento.valor) : '',
    descricao: lancamento?.descricao ?? '',
    categoria: lancamento?.categoria ?? '',
  }))
  const [categorias, setCategorias] = useState<Categoria[] | null>(null)
  const [erroCategorias, setErroCategorias] = useState(false)
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [removerAtual, setRemoverAtual] = useState(false)
  const [erros, setErros] = useState<Erros>({})
  const [tentou, setTentou] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erroServidor, setErroServidor] = useState<{ mensagem: string; conflito: boolean } | null>(null)
  const arquivoRef = useRef<HTMLInputElement>(null)
  const editando = lancamento !== null

  useEffect(() => {
    listarCategorias()
      .then(setCategorias)
      .catch(() => setErroCategorias(true))
  }, [])

  function mudar<K extends keyof Valores>(campo: K, valor: Valores[K]) {
    const proximos = { ...valores, [campo]: valor }
    setValores(proximos)
    if (tentou) setErros((atual) => ({ ...validar(proximos, hoje), arquivo: atual.arquivo }))
  }

  function escolherArquivo(lista: FileList | null) {
    const escolhido = lista?.[0] ?? null
    if (arquivoRef.current) arquivoRef.current.value = ''
    if (!escolhido) return
    const problema = validarArquivo(escolhido)
    setErros((atual) => ({ ...atual, arquivo: problema ?? undefined }))
    if (!problema) {
      setArquivo(escolhido)
      setRemoverAtual(false)
    }
  }

  const alterouDados =
    !editando ||
    valores.tipo !== lancamento.tipo ||
    valores.data !== lancamento.data ||
    valorParaApi(valores.valor) !== Number(lancamento.valor).toFixed(2) ||
    valores.descricao.trim() !== lancamento.descricao ||
    valores.categoria !== lancamento.categoria
  const alterouComprovante = arquivo !== null || removerAtual
  const podeSalvar = !enviando && (!editando || alterouDados || alterouComprovante)

  async function enviar(event: FormEvent) {
    event.preventDefault()
    setTentou(true)
    const encontrados = validar(valores, hoje)
    setErros((atual) => ({ ...encontrados, arquivo: atual.arquivo }))
    if (Object.keys(encontrados).length > 0) {
      const primeiro = Object.keys(encontrados)[0]
      document.getElementById(`${idBase}-${primeiro}`)?.focus()
      return
    }

    setEnviando(true)
    setErroServidor(null)
    const dados = {
      tipo: valores.tipo,
      data: valores.data,
      valor: valorParaApi(valores.valor),
      descricao: valores.descricao.trim(),
      categoria: valores.categoria,
    }

    let salvo: Lancamento
    try {
      if (!editando) {
        salvo = await criarLancamento({ ...dados, clienteId: valores.cliente!.id })
      } else if (alterouDados) {
        salvo = await atualizarLancamento(lancamento.id, { ...dados, versao: lancamento.versao })
      } else {
        salvo = lancamento
      }
    } catch (error) {
      setEnviando(false)
      setErroServidor({
        mensagem: getErrorMessage(error, 'Não foi possível salvar o lançamento.'),
        conflito: getErrorCode(error) === 'EDICAO_SIMULTANEA',
      })
      return
    }

    // Dados salvos; agora o comprovante, se mudou
    try {
      if (arquivo) {
        salvo = await enviarComprovante(salvo.id, salvo.versao, arquivo)
      } else if (removerAtual && salvo.comprovante) {
        salvo = await removerComprovante(salvo.id, salvo.versao)
      }
    } catch (error) {
      onAviso(
        `${editando ? 'Alterações salvas' : 'Lançamento salvo'}, mas o comprovante não foi ${
          arquivo ? 'enviado' : 'removido'
        }: ${getErrorMessage(error, 'tente de novo pelo menu do lançamento.')}`,
      )
      onSalvo(salvo, '')
      return
    }
    onSalvo(salvo, editando ? 'Lançamento atualizado.' : 'Lançamento salvo.')
  }

  const erroVisivel = (campo: Campo) => (tentou || campo === 'arquivo' ? erros[campo] : undefined)
  const id = (campo: string) => `${idBase}-${campo}`
  const comprovanteAtual = editando && lancamento.comprovante && !removerAtual && !arquivo ? lancamento.comprovante : null
  const formId = `${idBase}-form`

  return (
    <Dialog
      open
      title={editando ? 'Editar lançamento' : 'Novo lançamento'}
      onClose={onClose}
      busy={enviando}
      fecharPeloFundo={false}
      largura="larga"
      actions={
        <>
          <button type="button" className="ui-btn ui-btn--secondary" onClick={onClose} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" form={formId} className="ui-btn ui-btn--primary" disabled={!podeSalvar} aria-busy={enviando}>
            {enviando ? 'Salvando…' : editando ? 'Salvar alterações' : 'Salvar lançamento'}
          </button>
        </>
      }
    >
      <form id={formId} className="lc-form" onSubmit={(e) => void enviar(e)} noValidate>
        {erroServidor && (
          <div className="ui-alert ui-alert--error" role="alert">
            <AlertIcon />
            <div className="ui-alert__content">
              {erroServidor.mensagem}
              {erroServidor.conflito && (
                <div className="ui-alert__actions">
                  <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={onConflito}>
                    Fechar e recarregar
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {editando && lancamento.origem === 'CLIENTE' && (
          <div className="ui-alert ui-alert--info">
            <AlertIcon />
            <div className="ui-alert__content">
              Este lançamento foi feito pelo cliente no app. A alteração fica registrada no histórico.
            </div>
          </div>
        )}

        {!clienteFixo && (
          <div className="cf-field" data-invalid={Boolean(erroVisivel('cliente'))}>
            <label className="cf-label" htmlFor={id('cliente')}>
              Cliente
            </label>
            {editando ? (
              <p className="lc-form__fixo">{lancamento.clienteNome}</p>
            ) : (
              <ClienteSeletor
                id={id('cliente')}
                valor={valores.cliente}
                onChange={(c) => mudar('cliente', c)}
                invalido={Boolean(erroVisivel('cliente'))}
                descritoPor={erroVisivel('cliente') ? id('cliente-erro') : undefined}
              />
            )}
            {erroVisivel('cliente') && (
              <p className="cf-error" id={id('cliente-erro')}>
                {erroVisivel('cliente')}
              </p>
            )}
          </div>
        )}

        <fieldset className="cf-tipo lc-form__tipo">
          <legend className="cf-label">Tipo</legend>
          <div className="cf-segmented">
            {TIPOS.map((t) => (
              <label key={t.valor} className="cf-segmented__option">
                <input
                  type="radio"
                  name={id('tipo')}
                  value={t.valor}
                  checked={valores.tipo === t.valor}
                  onChange={() => mudar('tipo', t.valor)}
                />
                <span>{t.rotulo}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <div className="cf-grid">
          <div className="cf-field" data-invalid={Boolean(erroVisivel('data'))}>
            <label className="cf-label" htmlFor={id('data')}>
              Data
            </label>
            <input
              id={id('data')}
              type="date"
              className="cf-input"
              value={valores.data}
              min={DATA_MINIMA}
              max={hoje}
              onChange={(e) => mudar('data', e.target.value)}
              aria-invalid={Boolean(erroVisivel('data')) || undefined}
              aria-describedby={erroVisivel('data') ? id('data-erro') : undefined}
            />
            {erroVisivel('data') && (
              <p className="cf-error" id={id('data-erro')}>
                {erroVisivel('data')}
              </p>
            )}
          </div>

          <div className="cf-field" data-invalid={Boolean(erroVisivel('valor'))}>
            <label className="cf-label" htmlFor={id('valor')}>
              Valor
            </label>
            <div className="lc-moeda">
              <span className="lc-moeda__prefixo" aria-hidden="true">
                R$
              </span>
              <input
                id={id('valor')}
                className="cf-input lc-moeda__input"
                inputMode="numeric"
                placeholder="0,00"
                autoComplete="off"
                value={valores.valor}
                onChange={(e) => mudar('valor', mascararValor(e.target.value))}
                aria-invalid={Boolean(erroVisivel('valor')) || undefined}
                aria-describedby={erroVisivel('valor') ? id('valor-erro') : undefined}
              />
            </div>
            {erroVisivel('valor') && (
              <p className="cf-error" id={id('valor-erro')}>
                {erroVisivel('valor')}
              </p>
            )}
          </div>

          <div className="cf-field cf-field--wide" data-invalid={Boolean(erroVisivel('descricao'))}>
            <label className="cf-label" htmlFor={id('descricao')}>
              Descrição
            </label>
            <input
              id={id('descricao')}
              className="cf-input"
              maxLength={MAX_DESCRICAO}
              placeholder="Ex.: Aluguel da loja, compra de mercadorias"
              value={valores.descricao}
              onChange={(e) => mudar('descricao', e.target.value)}
              aria-invalid={Boolean(erroVisivel('descricao')) || undefined}
              aria-describedby={erroVisivel('descricao') ? id('descricao-erro') : undefined}
            />
            {erroVisivel('descricao') && (
              <p className="cf-error" id={id('descricao-erro')}>
                {erroVisivel('descricao')}
              </p>
            )}
          </div>

          <div className="cf-field cf-field--wide" data-invalid={Boolean(erroVisivel('categoria'))}>
            <label className="cf-label" htmlFor={id('categoria')}>
              Categoria
            </label>
            <select
              id={id('categoria')}
              className="cf-input"
              value={valores.categoria}
              onChange={(e) => mudar('categoria', e.target.value)}
              disabled={categorias === null}
              aria-invalid={Boolean(erroVisivel('categoria')) || undefined}
              aria-describedby={erroVisivel('categoria') ? id('categoria-erro') : undefined}
            >
              <option value="">{categorias === null ? (erroCategorias ? 'Erro ao carregar' : 'Carregando…') : 'Escolha…'}</option>
              {categorias?.map((c) => (
                <option key={c.valor} value={c.valor}>
                  {c.rotulo}
                </option>
              ))}
            </select>
            {erroCategorias && <p className="cf-error">Não foi possível carregar as categorias. Feche e abra de novo.</p>}
            {erroVisivel('categoria') && (
              <p className="cf-error" id={id('categoria-erro')}>
                {erroVisivel('categoria')}
              </p>
            )}
          </div>
        </div>

        <div className="cf-field" data-invalid={Boolean(erros.arquivo)}>
          <span className="cf-label" id={id('arquivo-rotulo')}>
            Comprovante <span className="cf-required">(opcional)</span>
          </span>
          <input
            ref={arquivoRef}
            id={id('arquivo')}
            type="file"
            className="sr-only"
            accept=".pdf,.jpg,.jpeg,.png,.webp,application/pdf,image/jpeg,image/png,image/webp"
            onChange={(e) => escolherArquivo(e.target.files)}
            aria-labelledby={id('arquivo-rotulo')}
            tabIndex={-1}
          />
          {comprovanteAtual || arquivo ? (
            <div className="lc-anexo">
              <PaperclipIcon width={18} height={18} className="lc-anexo__icone" />
              <span className="lc-anexo__nome">
                {arquivo ? arquivo.name : comprovanteAtual!.nome}
                <span className="lc-anexo__meta">
                  {formatarTamanho(arquivo ? arquivo.size : comprovanteAtual!.tamanho)}
                  {arquivo && editando && lancamento.comprovante ? ' · substitui o atual' : arquivo ? ' · será enviado ao salvar' : ''}
                </span>
              </span>
              <button type="button" className="ui-btn ui-btn--ghost ui-btn--sm" onClick={() => arquivoRef.current?.click()}>
                Trocar
              </button>
              <button
                type="button"
                className="ui-icon-btn"
                aria-label={arquivo ? 'Desistir deste arquivo' : 'Remover o comprovante'}
                onClick={() => {
                  if (arquivo) setArquivo(null)
                  else setRemoverAtual(true)
                }}
              >
                <TrashIcon width={18} height={18} />
              </button>
            </div>
          ) : (
            <button type="button" className="lc-anexar" onClick={() => arquivoRef.current?.click()}>
              <UploadIcon width={20} height={20} />
              <span>
                <strong>Anexar arquivo</strong>
                <span className="lc-anexar__dica">PDF, JPG, PNG ou WEBP, até 10 MB</span>
              </span>
            </button>
          )}
          {removerAtual && (
            <p className="cf-hint">
              O comprovante atual será removido ao salvar.{' '}
              <button type="button" className="lc-link" onClick={() => setRemoverAtual(false)}>
                Desfazer
              </button>
            </p>
          )}
          {erros.arquivo && <p className="cf-error">{erros.arquivo}</p>}
        </div>
      </form>
    </Dialog>
  )
}
