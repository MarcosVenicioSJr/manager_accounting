import { useEffect, useState } from 'react'
import { AlertIcon, DownloadIcon } from '../../components/icons'
import { Dialog } from '../../components/ui/Dialog'
import { getErrorMessage } from '../../services/errors'
import { formatarTamanho } from './formatar'
import { baixarComprovante } from './lancamentoService'
import type { Lancamento } from './types'

type Estado = { tipo: 'carregando' } | { tipo: 'erro'; mensagem: string } | { tipo: 'ok'; url: string }

// Mostra o comprovante (imagem ou PDF) dentro do backoffice. O arquivo vem pela API com o token, como blob,
// e vira um endereço local (blob:) que só existe enquanto o diálogo está aberto.
export function ComprovanteDialog({ lancamento, onClose }: { lancamento: Lancamento | null; onClose: () => void }) {
  if (!lancamento?.comprovante) return null
  return <Visualizador key={lancamento.id} lancamento={lancamento} onClose={onClose} />
}

function Visualizador({ lancamento, onClose }: { lancamento: Lancamento; onClose: () => void }) {
  const comprovante = lancamento.comprovante!
  const [estado, setEstado] = useState<Estado>({ tipo: 'carregando' })

  useEffect(() => {
    const controle = new AbortController()
    let url: string | null = null
    baixarComprovante(lancamento.id, controle.signal)
      .then((blob) => {
        // Usa o tipo que o backend detectou, não o que veio no blob
        url = URL.createObjectURL(new Blob([blob], { type: comprovante.tipo }))
        setEstado({ tipo: 'ok', url })
      })
      .catch((error: unknown) => {
        if (!controle.signal.aborted) {
          setEstado({ tipo: 'erro', mensagem: getErrorMessage(error, 'Não foi possível abrir o comprovante.') })
        }
      })
    return () => {
      controle.abort()
      if (url) URL.revokeObjectURL(url)
    }
  }, [lancamento.id, comprovante.tipo])

  const ehPdf = comprovante.tipo === 'application/pdf'

  return (
    <Dialog
      open
      title="Comprovante"
      onClose={onClose}
      largura="larga"
      actions={
        <>
          {estado.tipo === 'ok' && (
            <a className="ui-btn ui-btn--secondary" href={estado.url} download={comprovante.nome}>
              <DownloadIcon width={18} height={18} />
              Baixar
            </a>
          )}
          <button type="button" className="ui-btn ui-btn--primary" onClick={onClose} autoFocus>
            Fechar
          </button>
        </>
      }
    >
      <p className="lc-comprovante__info">
        {comprovante.nome} · {formatarTamanho(comprovante.tamanho)}
      </p>
      {estado.tipo === 'carregando' ? (
        <span className="ui-skeleton lc-comprovante__skeleton" role="status" aria-label="Carregando comprovante" />
      ) : estado.tipo === 'erro' ? (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">{estado.mensagem}</div>
        </div>
      ) : ehPdf ? (
        <iframe className="lc-comprovante__pdf" src={estado.url} title={`Comprovante: ${comprovante.nome}`} />
      ) : (
        <img className="lc-comprovante__img" src={estado.url} alt={`Comprovante do lançamento: ${lancamento.descricao}`} />
      )}
    </Dialog>
  )
}
